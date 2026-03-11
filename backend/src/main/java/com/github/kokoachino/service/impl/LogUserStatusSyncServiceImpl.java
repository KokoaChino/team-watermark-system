package com.github.kokoachino.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.kokoachino.common.enums.LogUserStatusEnum;
import com.github.kokoachino.mapper.BatchTaskMapper;
import com.github.kokoachino.mapper.PointChangeLogMapper;
import com.github.kokoachino.mapper.TeamEventLogMapper;
import com.github.kokoachino.mapper.WatermarkResourceLogMapper;
import com.github.kokoachino.model.entity.BatchTask;
import com.github.kokoachino.model.entity.PointChangeLog;
import com.github.kokoachino.model.entity.TeamEventLog;
import com.github.kokoachino.model.entity.WatermarkResourceLog;
import com.github.kokoachino.service.LogUserStatusSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * 日志用户状态同步服务实现
 *
 * @author Kokoa_Chino
 * @date 2026-03-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogUserStatusSyncServiceImpl implements LogUserStatusSyncService {

    private final TeamEventLogMapper teamEventLogMapper;
    private final WatermarkResourceLogMapper watermarkResourceLogMapper;
    private final PointChangeLogMapper pointChangeLogMapper;
    private final BatchTaskMapper batchTaskMapper;

    @Override
    public void syncUserStatus(Integer teamId, Integer userId, String username, LogUserStatusEnum targetStatus) {
        if (teamId == null || userId == null || username == null || username.isBlank() || targetStatus == null) {
            return;
        }
        String activeStatus = LogUserStatusEnum.ACTIVE.getValue();
        String targetValue = targetStatus.getValue();
        try {
            teamEventLogMapper.update(null, new LambdaUpdateWrapper<TeamEventLog>()
                    .eq(TeamEventLog::getTeamId, teamId)
                    .eq(TeamEventLog::getOperatorUserId, userId)
                    .eq(TeamEventLog::getOperatorUsername, username)
                    .eq(TeamEventLog::getOperatorUserStatus, activeStatus)
                    .set(TeamEventLog::getOperatorUserStatus, targetValue));
            teamEventLogMapper.update(null, new LambdaUpdateWrapper<TeamEventLog>()
                    .eq(TeamEventLog::getTeamId, teamId)
                    .eq(TeamEventLog::getAffectedUserId, userId)
                    .eq(TeamEventLog::getAffectedUsername, username)
                    .eq(TeamEventLog::getAffectedUserStatus, activeStatus)
                    .set(TeamEventLog::getAffectedUserStatus, targetValue));
            watermarkResourceLogMapper.update(null, new LambdaUpdateWrapper<WatermarkResourceLog>()
                    .eq(WatermarkResourceLog::getTeamId, teamId)
                    .eq(WatermarkResourceLog::getOperatorUserId, userId)
                    .eq(WatermarkResourceLog::getOperatorUsername, username)
                    .eq(WatermarkResourceLog::getOperatorUserStatus, activeStatus)
                    .set(WatermarkResourceLog::getOperatorUserStatus, targetValue));
            pointChangeLogMapper.update(null, new LambdaUpdateWrapper<PointChangeLog>()
                    .eq(PointChangeLog::getTeamId, teamId)
                    .eq(PointChangeLog::getOperatorUserId, userId)
                    .eq(PointChangeLog::getOperatorUsername, username)
                    .eq(PointChangeLog::getOperatorUserStatus, activeStatus)
                    .set(PointChangeLog::getOperatorUserStatus, targetValue));
            batchTaskMapper.update(null, new LambdaUpdateWrapper<BatchTask>()
                    .eq(BatchTask::getTeamId, teamId)
                    .eq(BatchTask::getCreatedById, userId)
                    .eq(BatchTask::getCreatedByUsername, username)
                    .eq(BatchTask::getUserStatus, activeStatus)
                    .set(BatchTask::getUserStatus, targetValue));
        } catch (Exception e) {
            log.error("同步日志中的用户状态失败，teamId={}, userId={}, username={}, targetStatus={}", teamId, userId, username, targetValue, e);
        }
    }
}
