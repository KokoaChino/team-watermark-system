package com.github.kokoachino.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kokoachino.common.enums.LogUserStatusEnum;
import com.github.kokoachino.common.enums.TeamEventTypeEnum;
import com.github.kokoachino.common.util.RequestIpUtils;
import com.github.kokoachino.common.util.TeamContext;
import com.github.kokoachino.mapper.TeamEventLogMapper;
import com.github.kokoachino.model.dto.TeamEventLogQueryDTO;
import com.github.kokoachino.model.dto.TeamEventLogRecordDTO;
import com.github.kokoachino.model.entity.TeamEventLog;
import com.github.kokoachino.model.vo.InviteRecordVO;
import com.github.kokoachino.model.vo.PageVO;
import com.github.kokoachino.model.vo.TeamEventLogVO;
import com.github.kokoachino.service.TeamEventLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 团队变更日志服务实现
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamEventLogServiceImpl implements TeamEventLogService {

    private final TeamEventLogMapper teamEventLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void record(TeamEventLogRecordDTO dto) {
        if (dto == null || dto.getTeamId() == null || dto.getEventType() == null) {
            return;
        }
        try {
            TeamEventLog logRecord = new TeamEventLog();
            logRecord.setTeamId(dto.getTeamId());
            logRecord.setEventType(dto.getEventType().getValue());
            logRecord.setOperatorUserId(dto.getOperatorUserId());
            logRecord.setOperatorUsername(dto.getOperatorUsername());
            logRecord.setOperatorUserStatus(dto.getOperatorUserStatus());
            logRecord.setAffectedUserId(dto.getAffectedUserId());
            logRecord.setAffectedUsername(dto.getAffectedUsername());
            logRecord.setAffectedUserStatus(dto.getAffectedUserStatus());
            logRecord.setInviteCodeId(dto.getInviteCodeId());
            logRecord.setInviteCode(dto.getInviteCode());
            if (dto.getBeforeData() != null) {
                logRecord.setBeforeData(objectMapper.writeValueAsString(dto.getBeforeData()));
            }
            if (dto.getAfterData() != null) {
                logRecord.setAfterData(objectMapper.writeValueAsString(dto.getAfterData()));
            }
            if (dto.getDetails() != null) {
                logRecord.setDetails(objectMapper.writeValueAsString(dto.getDetails()));
            }
            logRecord.setIpAddress(resolveClientIp());
            logRecord.setCreatedAt(LocalDateTime.now());
            teamEventLogMapper.insert(logRecord);
        } catch (Exception e) {
            log.error("记录团队变更日志失败", e);
        }
    }

    @Override
    public PageVO<TeamEventLogVO> queryLogs(TeamEventLogQueryDTO dto) {
        Integer teamId = TeamContext.getTeamId();
        Integer offset = (dto.getPage() - 1) * dto.getSize();
        List<TeamEventLog> logs = teamEventLogMapper.selectByConditions(
                teamId,
                dto.getEventType(),
                dto.getOperatorKeyword(),
                dto.getAffectedKeyword(),
                dto.getInviteCode(),
                dto.getStartTime(),
                dto.getEndTime(),
                offset,
                dto.getSize()
        );
        Long total = teamEventLogMapper.countByConditions(
                teamId,
                dto.getEventType(),
                dto.getOperatorKeyword(),
                dto.getAffectedKeyword(),
                dto.getInviteCode(),
                dto.getStartTime(),
                dto.getEndTime()
        );
        List<TeamEventLogVO> list = logs.stream().map(this::convertToVO).toList();
        return PageVO.<TeamEventLogVO>builder().list(list).total(total).page(dto.getPage()).size(dto.getSize()).build();
    }

    @Override
    public List<String> listUsernames(String field, String keyword) {
        Integer teamId = TeamContext.getTeamId();
        if ("affected".equalsIgnoreCase(field)) {
            return teamEventLogMapper.selectDistinctAffectedUsernames(teamId, keyword);
        }
        return teamEventLogMapper.selectDistinctOperatorUsernames(teamId, keyword);
    }

    @Override
    public List<InviteRecordVO> getInviteRecords(Integer codeId) {
        Integer teamId = TeamContext.getTeamId();
        List<TeamEventLog> logs = teamEventLogMapper.selectInviteRecordsByInviteCodeId(teamId, codeId);
        return logs.stream().map(logRecord -> InviteRecordVO.builder()
                .id(logRecord.getId())
                .inviteCode(logRecord.getInviteCode())
                .userId(logRecord.getAffectedUserId() != null ? logRecord.getAffectedUserId() : logRecord.getOperatorUserId())
                .username(logRecord.getAffectedUsername() != null ? logRecord.getAffectedUsername() : logRecord.getOperatorUsername())
                .joinedAt(logRecord.getCreatedAt())
                .build()).toList();
    }

    private TeamEventLogVO convertToVO(TeamEventLog logRecord) {
        TeamEventTypeEnum eventType = TeamEventTypeEnum.fromValue(logRecord.getEventType());
        LogUserStatusEnum operatorStatus = LogUserStatusEnum.fromValue(logRecord.getOperatorUserStatus());
        LogUserStatusEnum affectedStatus = LogUserStatusEnum.fromValue(logRecord.getAffectedUserStatus());
        return TeamEventLogVO.builder()
                .id(logRecord.getId())
                .eventType(logRecord.getEventType())
                .eventTypeDesc(eventType != null ? eventType.getDescription() : logRecord.getEventType())
                .operatorUserId(logRecord.getOperatorUserId())
                .operatorUsername(logRecord.getOperatorUsername())
                .operatorUserStatus(logRecord.getOperatorUserStatus())
                .operatorUserStatusDesc(operatorStatus != null ? operatorStatus.getDescription() : null)
                .affectedUserId(logRecord.getAffectedUserId())
                .affectedUsername(logRecord.getAffectedUsername())
                .affectedUserStatus(logRecord.getAffectedUserStatus())
                .affectedUserStatusDesc(affectedStatus != null ? affectedStatus.getDescription() : null)
                .inviteCodeId(logRecord.getInviteCodeId())
                .inviteCode(logRecord.getInviteCode())
                .beforeData(logRecord.getBeforeData())
                .afterData(logRecord.getAfterData())
                .details(logRecord.getDetails())
                .ipAddress(logRecord.getIpAddress())
                .createdAt(logRecord.getCreatedAt())
                .build();
    }

    private String resolveClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();
        return RequestIpUtils.getClientIp(request);
    }
}
