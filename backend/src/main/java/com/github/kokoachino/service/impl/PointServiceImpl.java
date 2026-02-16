package com.github.kokoachino.service.impl;

import com.github.kokoachino.common.enums.EventTypeEnum;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.mapper.TeamMapper;
import com.github.kokoachino.model.entity.Team;
import com.github.kokoachino.service.OperationLogService;
import com.github.kokoachino.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


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
    private final RedissonClient redissonClient;
    private final OperationLogService operationLogService;

    private static final String POINT_LOCK_PREFIX = "point:lock:";

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
                team.setPointBalance(balanceAfter);
                teamMapper.updateById(team);
                log.info("预扣点数成功：teamId={}, points={}, balanceAfter={}", teamId, points, balanceAfter);
                Map<String, Object> details = new HashMap<>();
                details.put("points", points);
                details.put("balanceBefore", balanceBefore);
                details.put("balanceAfter", balanceAfter);
                details.put("bizType", bizType);
                details.put("bizId", bizId);
                operationLogService.log(EventTypeEnum.POINT_DEDUCT, teamId, userId, null, null, description, null, null, details);
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
                team.setPointBalance(balanceAfter);
                teamMapper.updateById(team);
                log.info("返还点数成功：teamId={}, points={}, balanceAfter={}", teamId, points, balanceAfter);
                Map<String, Object> details = new HashMap<>();
                details.put("points", points);
                details.put("balanceBefore", balanceBefore);
                details.put("balanceAfter", balanceAfter);
                details.put("bizType", bizType);
                details.put("bizId", bizId);
                operationLogService.log(EventTypeEnum.POINT_REFUND, teamId, userId, null, null, description, null, null, details);
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
                team.setPointBalance(balanceAfter);
                teamMapper.updateById(team);
                log.info("充值点数成功：teamId={}, points={}, balanceAfter={}", teamId, points, balanceAfter);
                Map<String, Object> details = new HashMap<>();
                details.put("points", points);
                details.put("balanceBefore", balanceBefore);
                details.put("balanceAfter", balanceAfter);
                details.put("bizType", bizType);
                details.put("bizId", bizId);
                operationLogService.log(EventTypeEnum.POINT_RECHARGE, teamId, userId, null, null, description, null, null, details);
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
    public boolean hasEnoughPoints(Integer teamId, Integer points) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            return false;
        }
        return team.getPointBalance() >= points;
    }
}
