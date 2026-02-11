package com.github.kokoachino.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.kokoachino.common.enums.EventTypeEnum;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.mapper.PointTransactionMapper;
import com.github.kokoachino.mapper.TeamMapper;
import com.github.kokoachino.model.entity.PointTransaction;
import com.github.kokoachino.model.entity.Team;
import com.github.kokoachino.model.vo.PageVO;
import com.github.kokoachino.model.vo.PointBalanceVO;
import com.github.kokoachino.model.vo.PointTransactionVO;
import com.github.kokoachino.service.OperationLogService;
import com.github.kokoachino.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 点数服务实现
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final TeamMapper teamMapper;
    private final PointTransactionMapper pointTransactionMapper;
    private final RedissonClient redissonClient;
    private final OperationLogService operationLogService;

    private static final String POINT_LOCK_PREFIX = "point:lock:";

    @Override
    public PointBalanceVO getBalance(Integer teamId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BizException(ResultCode.TEAM_NOT_FOUND);
        }
        return PointBalanceVO.builder()
                .teamId(team.getId())
                .teamName(team.getName())
                .balance(team.getPointBalance())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductPoints(Integer teamId, Integer userId, Integer points, String bizType, String bizId, String description) {
        if (points <= 0) {
            return true;
        }
        String lockKey = POINT_LOCK_PREFIX + teamId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
            }
            try {
                Team team = teamMapper.selectById(teamId);
                if (team == null) {
                    throw new BizException(ResultCode.TEAM_NOT_FOUND);
                }
                if (team.getPointBalance() < points) {
                    throw new BizException(ResultCode.POINTS_NOT_ENOUGH);
                }
                int balanceBefore = team.getPointBalance();
                int balanceAfter = balanceBefore - points;
                // 更新余额
                team.setPointBalance(balanceAfter);
                teamMapper.updateById(team);
                // 记录流水
                PointTransaction transaction = new PointTransaction();
                transaction.setTeamId(teamId);
                transaction.setUserId(userId);
                transaction.setType("deduct");
                transaction.setPoints(-points);
                transaction.setBalanceBefore(balanceBefore);
                transaction.setBalanceAfter(balanceAfter);
                transaction.setBizType(bizType);
                transaction.setBizId(bizId);
                transaction.setDescription(description);
                transaction.setCreatedAt(LocalDateTime.now());
                transaction.setUpdatedAt(LocalDateTime.now());
                pointTransactionMapper.insert(transaction);
                log.info("预扣点数成功：teamId={}, points={}, balanceAfter={}", teamId, points, balanceAfter);
                // 记录操作日志
                Map<String, Object> details = new HashMap<>();
                details.put("points", points);
                details.put("balanceBefore", balanceBefore);
                details.put("balanceAfter", balanceAfter);
                details.put("bizType", bizType);
                details.put("bizId", bizId);
                operationLogService.log(EventTypeEnum.POINT_DEDUCT, null, description, details);
                return true;
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
    public boolean refundPoints(Integer teamId, Integer userId, Integer points, String bizType, String bizId, String description) {
        if (points <= 0) {
            return true;
        }
        String lockKey = POINT_LOCK_PREFIX + teamId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
            }
            try {
                Team team = teamMapper.selectById(teamId);
                if (team == null) {
                    throw new BizException(ResultCode.TEAM_NOT_FOUND);
                }
                int balanceBefore = team.getPointBalance();
                int balanceAfter = balanceBefore + points;
                // 更新余额
                team.setPointBalance(balanceAfter);
                teamMapper.updateById(team);
                // 记录流水
                PointTransaction transaction = new PointTransaction();
                transaction.setTeamId(teamId);
                transaction.setUserId(userId);
                transaction.setType("refund");
                transaction.setPoints(points);
                transaction.setBalanceBefore(balanceBefore);
                transaction.setBalanceAfter(balanceAfter);
                transaction.setBizType(bizType);
                transaction.setBizId(bizId);
                transaction.setDescription(description);
                transaction.setCreatedAt(LocalDateTime.now());
                transaction.setUpdatedAt(LocalDateTime.now());
                pointTransactionMapper.insert(transaction);
                log.info("返还点数成功：teamId={}, points={}, balanceAfter={}", teamId, points, balanceAfter);
                // 记录操作日志
                Map<String, Object> details = new HashMap<>();
                details.put("points", points);
                details.put("balanceBefore", balanceBefore);
                details.put("balanceAfter", balanceAfter);
                details.put("bizType", bizType);
                details.put("bizId", bizId);
                operationLogService.log(EventTypeEnum.POINT_REFUND, null, description, details);
                return true;
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
    public boolean rechargePoints(Integer teamId, Integer userId, Integer points, String bizType, String bizId, String description) {
        if (points <= 0) {
            return true;
        }
        String lockKey = POINT_LOCK_PREFIX + teamId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
            }
            try {
                Team team = teamMapper.selectById(teamId);
                if (team == null) {
                    throw new BizException(ResultCode.TEAM_NOT_FOUND);
                }
                int balanceBefore = team.getPointBalance();
                int balanceAfter = balanceBefore + points;
                // 更新余额
                team.setPointBalance(balanceAfter);
                teamMapper.updateById(team);
                // 记录流水
                PointTransaction transaction = new PointTransaction();
                transaction.setTeamId(teamId);
                transaction.setUserId(userId);
                transaction.setType("recharge");
                transaction.setPoints(points);
                transaction.setBalanceBefore(balanceBefore);
                transaction.setBalanceAfter(balanceAfter);
                transaction.setBizType(bizType);
                transaction.setBizId(bizId);
                transaction.setDescription(description);
                transaction.setCreatedAt(LocalDateTime.now());
                transaction.setUpdatedAt(LocalDateTime.now());
                pointTransactionMapper.insert(transaction);
                log.info("充值点数成功：teamId={}, points={}, balanceAfter={}", teamId, points, balanceAfter);
                // 记录操作日志
                Map<String, Object> details = new HashMap<>();
                details.put("points", points);
                details.put("balanceBefore", balanceBefore);
                details.put("balanceAfter", balanceAfter);
                details.put("bizType", bizType);
                details.put("bizId", bizId);
                operationLogService.log(EventTypeEnum.POINT_RECHARGE, null, description, details);
                return true;
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
        }
    }

    @Override
    public PageVO<PointTransactionVO> getTransactions(Integer teamId, Integer page, Integer size) {
        // 计算偏移量
        int offset = (page - 1) * size;
        // 查询总数
        LambdaQueryWrapper<PointTransaction> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(PointTransaction::getTeamId, teamId);
        Long total = pointTransactionMapper.selectCount(countWrapper);
        // 查询数据
        LambdaQueryWrapper<PointTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointTransaction::getTeamId, teamId)
                .orderByDesc(PointTransaction::getCreatedAt)
                .last("LIMIT " + offset + ", " + size);
        List<PointTransaction> transactions = pointTransactionMapper.selectList(wrapper);
        // 转换为VO
        List<PointTransactionVO> voList = transactions.stream()
                .map(t -> PointTransactionVO.builder()
                        .id(t.getId())
                        .type(t.getType())
                        .points(t.getPoints())
                        .balanceBefore(t.getBalanceBefore())
                        .balanceAfter(t.getBalanceAfter())
                        .bizType(t.getBizType())
                        .description(t.getDescription())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        return PageVO.<PointTransactionVO>builder()
                .list(voList)
                .total(total)
                .page(page)
                .size(size)
                .build();
    }

    @Override
    public boolean hasEnoughPoints(Integer teamId, Integer points) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            return false;
        }
        return team.getPointBalance() >= points;
    }
}
