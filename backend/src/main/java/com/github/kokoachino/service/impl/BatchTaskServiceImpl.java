package com.github.kokoachino.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kokoachino.common.enums.LogUserStatusEnum;
import com.github.kokoachino.common.enums.PointSourceTypeEnum;
import com.github.kokoachino.common.enums.TaskLogStatusEnum;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.common.util.RandomStringUtils;
import com.github.kokoachino.common.util.TeamContext;
import com.github.kokoachino.common.util.UserContext;
import com.github.kokoachino.config.SystemProperties;
import com.github.kokoachino.mapper.BatchTaskMapper;
import com.github.kokoachino.model.dto.SubmitBatchTaskDTO;
import com.github.kokoachino.model.dto.TaskLogQueryDTO;
import com.github.kokoachino.model.entity.BatchTask;
import com.github.kokoachino.model.vo.BatchTaskVO;
import com.github.kokoachino.model.vo.PageVO;
import com.github.kokoachino.model.vo.TaskLogVO;
import com.github.kokoachino.service.BatchTaskService;
import com.github.kokoachino.service.MinioService;
import com.github.kokoachino.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


/**
 * 批量任务服务实现
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchTaskServiceImpl implements BatchTaskService {

    private static final String TASK_LOCK_PREFIX = "batch:task:user:";

    private final BatchTaskMapper batchTaskMapper;
    private final PointService pointService;
    private final MinioService minioService;
    private final SystemProperties systemProperties;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchTaskVO submitTask(SubmitBatchTaskDTO dto) {
        Integer userId = UserContext.getUserId();
        Integer teamId = TeamContext.getTeamId();
        String username = UserContext.getUser().getUsername();
        if (dto.getTotalCount() > systemProperties.getBatchTask().getMaxImagesPerTask()) {
            throw new BizException(ResultCode.VALIDATE_FAILED);
        }
        String lockKey = TASK_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
            }
            try {
                int uncompletedCount = batchTaskMapper.countUncompletedByUserId(userId);
                if (uncompletedCount > 0) {
                    throw new BizException(ResultCode.BATCH_TASK_SUBMIT_FAILED);
                }
                int totalPoints = dto.getTotalCount();
                if (!pointService.hasEnoughPoints(teamId, totalPoints)) {
                    throw new BizException(ResultCode.POINTS_NOT_ENOUGH);
                }
                String taskNo = generateTaskNo();
                boolean deductSuccess = pointService.deductPoints(teamId, userId, totalPoints, PointSourceTypeEnum.BATCH_TASK.getValue(), taskNo, "批量任务预扣点数：" + totalPoints + "点");
                if (!deductSuccess) {
                    throw new BizException(ResultCode.POINT_TRANSACTION_FAILED);
                }
                BatchTask task = new BatchTask();
                task.setTaskNo(taskNo);
                task.setTeamId(teamId);
                task.setCreatedById(userId);
                task.setCreatedByUsername(username);
                task.setUserStatus(LogUserStatusEnum.ACTIVE.getValue());
                task.setTemplateId(dto.getTemplateId());
                task.setTemplateName(dto.getTemplateName());
                task.setTemplateVersion(dto.getTemplateVersion());
                task.setTemplateSnapshot(objectMapper.writeValueAsString(dto.getTemplateSnapshot()));
                task.setDescription(dto.getDescription());
                task.setTotalCount(dto.getTotalCount());
                task.setSuccessCount(0);
                task.setFailedCount(0);
                task.setTotalDurationMs(0L);
                task.setTotalSize(dto.getTotalSize());
                task.setStartedAt(LocalDateTime.now());
                task.setCreatedAt(LocalDateTime.now());
                batchTaskMapper.insert(task);
                return convertToVO(task);
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("提交批量任务失败", e);
            throw new BizException(ResultCode.BATCH_TASK_SUBMIT_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Integer taskId, Integer successCount, MultipartFile resultZip, String reportJson) {
        Integer userId = UserContext.getUserId();
        String lockKey = TASK_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
            }
            try {
                BatchTask task = batchTaskMapper.selectById(taskId);
                if (task == null) {
                    throw new BizException(ResultCode.BATCH_TASK_NOT_FOUND);
                }
                if (!task.getCreatedById().equals(userId)) {
                    throw new BizException(ResultCode.FORBIDDEN);
                }
                if (task.getFinishedAt() != null) {
                    throw new BizException(ResultCode.TASK_ALREADY_COMPLETED);
                }
                if (successCount > task.getTotalCount()) {
                    throw new BizException(ResultCode.VALIDATE_FAILED);
                }
                String resultZipKey = null;
                if (resultZip != null && !resultZip.isEmpty()) {
                    try {
                        String objectName = "batch-results/" + task.getTaskNo() + "/result.zip";
                        resultZipKey = minioService.uploadFile(resultZip, objectName);
                    } catch (Exception e) {
                        log.error("结果ZIP文件上传失败：taskId={}", taskId, e);
                    }
                }
                int refundedPoints = task.getTotalCount() - successCount;
                pointService.refundPoints(task.getTeamId(), userId, refundedPoints, PointSourceTypeEnum.BATCH_TASK.getValue(), task.getTaskNo(), "批量任务返还点数：" + refundedPoints + "点");
                TaskReportSummary summary = parseReportSummary(reportJson);
                task.setSuccessCount(successCount);
                task.setFailedCount(task.getTotalCount() - successCount);
                task.setTotalDurationMs(summary.totalDurationMs());
                task.setResultZipKey(resultZipKey);
                task.setReport(reportJson);
                task.setStartedAt(summary.startedAt() != null ? summary.startedAt() : task.getStartedAt());
                task.setFinishedAt(summary.finishedAt() != null ? summary.finishedAt() : LocalDateTime.now());
                batchTaskMapper.updateById(task);
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
        }
    }

    @Override
    public PageVO<TaskLogVO> queryTaskLogs(TaskLogQueryDTO dto) {
        Integer teamId = TeamContext.getTeamId();
        Integer offset = (dto.getPage() - 1) * dto.getSize();
        List<TaskLogVO> list = batchTaskMapper.selectLogPage(
                        teamId,
                        dto.getStatus(),
                        dto.getOperatorKeyword(),
                        dto.getTemplateName(),
                        dto.getTaskNo(),
                        dto.getStartTime(),
                        dto.getEndTime(),
                        offset,
                        dto.getSize()
                )
                .stream()
                .map(this::fillDescriptions)
                .toList();
        Long total = batchTaskMapper.countLogPage(
                teamId,
                dto.getStatus(),
                dto.getOperatorKeyword(),
                dto.getTemplateName(),
                dto.getTaskNo(),
                dto.getStartTime(),
                dto.getEndTime()
        );
        return PageVO.<TaskLogVO>builder().list(list).total(total).page(dto.getPage()).size(dto.getSize()).build();
    }

    @Override
    public List<String> listTaskUsernames(String keyword) {
        Integer teamId = TeamContext.getTeamId();
        return batchTaskMapper.selectDistinctCreatedByUsernames(teamId, keyword);
    }

    private TaskLogVO fillDescriptions(TaskLogVO vo) {
        TaskLogStatusEnum status = TaskLogStatusEnum.fromValue(vo.getStatus());
        LogUserStatusEnum userStatus = LogUserStatusEnum.fromValue(vo.getUserStatus());
        vo.setStatusDesc(status != null ? status.getDescription() : vo.getStatus());
        vo.setUserStatusDesc(userStatus != null ? userStatus.getDescription() : null);
        return vo;
    }

    private BatchTaskVO convertToVO(BatchTask task) {
        return BatchTaskVO.builder()
                .id(task.getId())
                .taskNo(task.getTaskNo())
                .totalCount(task.getTotalCount())
                .templateId(task.getTemplateId())
                .templateName(task.getTemplateName())
                .createdAt(task.getCreatedAt())
                .build();
    }

    private TaskReportSummary parseReportSummary(String reportJson) {
        if (reportJson == null || reportJson.isBlank()) {
            return new TaskReportSummary(null, null, 0L);
        }
        try {
            JsonNode root = objectMapper.readTree(reportJson);
            LocalDateTime startedAt = parseDateTime(root.path("startedAt").asText(null));
            LocalDateTime finishedAt = parseDateTime(root.path("finishedAt").asText(null));
            long totalDurationMs = 0L;
            JsonNode items = root.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    totalDurationMs += item.path("durationMs").asLong(0L);
                }
            }
            return new TaskReportSummary(startedAt, finishedAt, totalDurationMs);
        } catch (Exception e) {
            log.warn("解析任务报表失败，使用默认统计信息", e);
            return new TaskReportSummary(null, null, 0L);
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.replace("Z", ""));
    }

    private String generateTaskNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomNum = String.format("%05d", ThreadLocalRandom.current().nextInt(100000));
        String randomStr = RandomStringUtils.generate(12);
        return String.format("TSK%s-%s-%s", randomNum, date, randomStr);
    }

    private record TaskReportSummary(LocalDateTime startedAt, LocalDateTime finishedAt, Long totalDurationMs) {}
}
