package com.github.kokoachino.service.impl;

import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.common.util.UserContext;
import com.github.kokoachino.config.SystemProperties;
import com.github.kokoachino.mapper.BatchTaskMapper;
import com.github.kokoachino.model.dto.batch.SubmitBatchTaskDTO;
import com.github.kokoachino.model.entity.BatchTask;
import com.github.kokoachino.model.enums.EventType;
import com.github.kokoachino.model.vo.BatchTaskVO;
import com.github.kokoachino.service.BatchTaskService;
import com.github.kokoachino.service.MinioService;
import com.github.kokoachino.service.OperationLogService;
import com.github.kokoachino.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;


/**
 * 批量任务服务实现（简化版）
 * 前端负责执行任务，后端只负责点数管理和结算
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchTaskServiceImpl implements BatchTaskService {

    private final BatchTaskMapper batchTaskMapper;
    private final PointService pointService;
    private final MinioService minioService;
    private final SystemProperties systemProperties;
    private final RedissonClient redissonClient;
    private final OperationLogService operationLogService;

    private static final String TASK_LOCK_PREFIX = "batch:task:user:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchTaskVO submitTask(SubmitBatchTaskDTO dto) {
        Integer userId = UserContext.getUserId();
        Integer teamId = UserContext.getUser().getTeamId();
        // 1. 检查是否超过单任务最大图片数
        if (dto.getImageCount() > systemProperties.getBatchTask().getMaxImagesPerTask()) {
            throw new BizException(ResultCode.VALIDATE_FAILED);
        }
        // 2. 使用分布式锁确保同一用户只有一个进行中的任务
        String lockKey = TASK_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
            }
            try {
                // 3. 检查用户是否有未完成的任务
                int uncompletedCount = batchTaskMapper.countUncompletedByUserId(userId);
                if (uncompletedCount > 0) {
                    throw new BizException(ResultCode.BATCH_TASK_SUBMIT_FAILED);
                }
                // 4. 验证点数是否充足
                int totalPoints = dto.getImageCount();
                if (!pointService.hasEnoughPoints(teamId, totalPoints)) {
                    throw new BizException(ResultCode.POINTS_NOT_ENOUGH);
                }
                // 5. 预扣点数
                String taskNo = generateTaskNo();
                boolean deductSuccess = pointService.deductPoints(
                        teamId, userId, totalPoints,
                        "batch_task", taskNo,
                        "批量任务预扣点数：" + totalPoints + "点"
                );
                if (!deductSuccess) {
                    throw new BizException(ResultCode.POINT_TRANSACTION_FAILED);
                }
                // 6. 创建任务记录
                BatchTask task = new BatchTask();
                task.setTaskNo(taskNo);
                task.setTeamId(teamId);
                task.setCreatedById(userId);
                task.setDescription(dto.getDescription());
                task.setImageCount(dto.getImageCount());
                task.setSuccessCount(0);
                task.setDeductedPoints(totalPoints);
                task.setConsumedPoints(0);
                task.setRefundedPoints(0);
                task.setCreatedAt(LocalDateTime.now());
                task.setUpdatedAt(LocalDateTime.now());
                batchTaskMapper.insert(task);
                log.info("批量任务提交成功：taskId={}, taskNo={}, imageCount={}, deductedPoints={}",
                        task.getId(), taskNo, dto.getImageCount(), totalPoints);
                // 记录操作日志
                operationLogService.log(EventType.BATCH_TASK_SUBMIT, task.getId(), taskNo,
                        Map.of("imageCount", dto.getImageCount(), "deductedPoints", totalPoints, "description", dto.getDescription()));
                return convertToVO(task);
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Integer taskId, Integer successCount, MultipartFile resultZip, String reportJson) {
        Integer userId = UserContext.getUserId();
        // 1. 使用分布式锁
        String lockKey = TASK_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
            }
            try {
                // 2. 查询任务
                BatchTask task = batchTaskMapper.selectById(taskId);
                if (task == null) {
                    throw new BizException(ResultCode.BATCH_TASK_NOT_FOUND);
                }
                // 3. 验证权限
                if (!task.getCreatedById().equals(userId)) {
                    throw new BizException(ResultCode.FORBIDDEN);
                }
                // 4. 检查任务是否已完成
                if (task.getCompletedAt() != null) {
                    throw new BizException(ResultCode.TASK_ALREADY_COMPLETED);
                }
                // 5. 验证成功数量不超过图片总数
                if (successCount > task.getImageCount()) {
                    throw new BizException(ResultCode.VALIDATE_FAILED);
                }
                // 6. 上传结果ZIP文件到MinIO（如果提供了）
                String resultZipKey = null;
                if (resultZip != null && !resultZip.isEmpty()) {
                    try {
                        // 生成带过期时间的对象Key
                        String objectName = "batch-results/" + task.getTaskNo() + "/result.zip";
                        resultZipKey = minioService.uploadFile(resultZip, objectName);
                        log.info("结果ZIP文件上传成功：taskId={}, key={}", taskId, resultZipKey);
                    } catch (Exception e) {
                        log.error("结果ZIP文件上传失败：taskId={}", taskId, e);
                        // 上传失败不影响点数结算
                    }
                }
                // 7. 计算实际消耗和返还点数
                int consumedPoints = successCount;
                int refundedPoints = task.getDeductedPoints() - consumedPoints;
                // 8. 返还多余点数
                if (refundedPoints > 0) {
                    pointService.refundPoints(
                            task.getTeamId(), userId, refundedPoints,
                            "batch_task", task.getTaskNo(),
                            "批量任务返还点数：" + refundedPoints + "点"
                    );
                }
                // 9. 更新任务记录
                task.setSuccessCount(successCount);
                task.setConsumedPoints(consumedPoints);
                task.setRefundedPoints(refundedPoints);
                task.setResultZipKey(resultZipKey);
                task.setReport(reportJson);
                task.setCompletedAt(LocalDateTime.now());
                task.setUpdatedAt(LocalDateTime.now());
                batchTaskMapper.updateById(task);
                log.info("批量任务完成：taskId={}, successCount={}, consumedPoints={}, refundedPoints={}",
                        taskId, successCount, consumedPoints, refundedPoints);
                // 记录操作日志
                operationLogService.log(EventType.BATCH_TASK_COMPLETE, taskId, task.getTaskNo(),
                        Map.of("successCount", successCount, "consumedPoints", consumedPoints, "refundedPoints", refundedPoints));
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
        }
    }

    /**
     * 生成任务编号
     */
    private String generateTaskNo() {
        return "BT" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }

    /**
     * 转换为 VO
     */
    private BatchTaskVO convertToVO(BatchTask task) {
        return BatchTaskVO.builder()
                .id(task.getId())
                .taskNo(task.getTaskNo())
                .deductedPoints(task.getDeductedPoints())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
