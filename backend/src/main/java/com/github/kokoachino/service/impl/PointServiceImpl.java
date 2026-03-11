package com.github.kokoachino.service.impl;

import com.github.kokoachino.common.enums.LogUserStatusEnum;
import com.github.kokoachino.common.enums.PointChangeTypeEnum;
import com.github.kokoachino.common.enums.PointSourceTypeEnum;
import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import com.github.kokoachino.mapper.TeamMapper;
import com.github.kokoachino.mapper.UserMapper;
import com.github.kokoachino.model.dto.PointChangeLogRecordDTO;
import com.github.kokoachino.model.entity.Team;
import com.github.kokoachino.model.entity.User;
import com.github.kokoachino.service.PointChangeLogService;
import com.github.kokoachino.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;


/**
 * 点数服务实现
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private static final String POINT_LOCK_PREFIX = "point:lock:";

    private final TeamMapper teamMapper;
    private final UserMapper userMapper;
    private final RedissonClient redissonClient;
    private final PointChangeLogService pointChangeLogService;

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
                log.info("预扣点数成功，teamId={}, points={}, balanceAfter={}", teamId, points, balanceAfter);
                recordPointChange(PointChangeTypeEnum.DEDUCT, teamId, userId, points, balanceBefore, balanceAfter, bizType, bizId, description);
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
        if (points < 0) {
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
                if (points > 0) {
                    team.setPointBalance(balanceAfter);
                    teamMapper.updateById(team);
                    log.info("返还点数成功，teamId={}, points={}, balanceAfter={}", teamId, points, balanceAfter);
                } else {
                    log.info("记录0点返还日志，teamId={}, bizId={}", teamId, bizId);
                }
                recordPointChange(PointChangeTypeEnum.REFUND, teamId, userId, points, balanceBefore, balanceAfter, bizType, bizId, description);
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
                log.info("充值点数成功，teamId={}, points={}, balanceAfter={}", teamId, points, balanceAfter);
                recordPointChange(PointChangeTypeEnum.RECHARGE, teamId, userId, points, balanceBefore, balanceAfter, bizType, bizId, description);
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

    private void recordPointChange(PointChangeTypeEnum changeType,
                                   Integer teamId,
                                   Integer userId,
                                   Integer points,
                                   Integer balanceBefore,
                                   Integer balanceAfter,
                                   String bizType,
                                   String bizId,
                                   String description) {
        User operator = userId == null ? null : userMapper.selectById(userId);
        String operatorStatus = null;
        if (userId != null) {
            operatorStatus = operator != null ? LogUserStatusEnum.ACTIVE.getValue() : LogUserStatusEnum.DELETED.getValue();
        }
        pointChangeLogService.record(PointChangeLogRecordDTO.builder()
                .teamId(teamId)
                .changeType(changeType)
                .operatorUserId(userId)
                .operatorUsername(operator != null ? operator.getUsername() : null)
                .operatorUserStatus(operatorStatus)
                .sourceType(PointSourceTypeEnum.fromValue(bizType))
                .sourceId(bizId)
                .points(points)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(description)
                .build());
    }
}