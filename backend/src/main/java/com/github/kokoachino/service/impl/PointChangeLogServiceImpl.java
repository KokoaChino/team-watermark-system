package com.github.kokoachino.service.impl;

import com.github.kokoachino.common.enums.LogUserStatusEnum;
import com.github.kokoachino.common.enums.PointChangeTypeEnum;
import com.github.kokoachino.common.enums.PointSourceTypeEnum;
import com.github.kokoachino.common.util.RequestIpUtils;
import com.github.kokoachino.common.util.TeamContext;
import com.github.kokoachino.mapper.PointChangeLogMapper;
import com.github.kokoachino.model.dto.PointChangeLogQueryDTO;
import com.github.kokoachino.model.dto.PointChangeLogRecordDTO;
import com.github.kokoachino.model.entity.PointChangeLog;
import com.github.kokoachino.model.vo.PageVO;
import com.github.kokoachino.model.vo.PointChangeLogVO;
import com.github.kokoachino.service.PointChangeLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 点数流水日志服务实现
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointChangeLogServiceImpl implements PointChangeLogService {

    private final PointChangeLogMapper pointChangeLogMapper;

    @Override
    public void record(PointChangeLogRecordDTO dto) {
        if (dto == null || dto.getTeamId() == null || dto.getChangeType() == null) {
            return;
        }
        try {
            PointChangeLog logRecord = new PointChangeLog();
            logRecord.setTeamId(dto.getTeamId());
            logRecord.setChangeType(dto.getChangeType().getValue());
            logRecord.setOperatorUserId(dto.getOperatorUserId());
            logRecord.setOperatorUsername(dto.getOperatorUsername());
            logRecord.setOperatorUserStatus(dto.getOperatorUserStatus());
            logRecord.setSourceType(dto.getSourceType() == null ? null : dto.getSourceType().getValue());
            logRecord.setSourceId(dto.getSourceId());
            logRecord.setPoints(dto.getPoints());
            logRecord.setBalanceBefore(dto.getBalanceBefore());
            logRecord.setBalanceAfter(dto.getBalanceAfter());
            logRecord.setDescription(dto.getDescription());
            logRecord.setIpAddress(resolveClientIp());
            logRecord.setCreatedAt(LocalDateTime.now());
            pointChangeLogMapper.insert(logRecord);
        } catch (Exception e) {
            log.error("记录点数流水日志失败", e);
        }
    }

    @Override
    public PageVO<PointChangeLogVO> queryLogs(PointChangeLogQueryDTO dto) {
        Integer teamId = TeamContext.getTeamId();
        Integer offset = (dto.getPage() - 1) * dto.getSize();
        List<PointChangeLog> logs = pointChangeLogMapper.selectByConditions(
                teamId,
                dto.getChangeType(),
                dto.getSourceType(),
                dto.getSourceId(),
                dto.getOperatorKeyword(),
                dto.getStartTime(),
                dto.getEndTime(),
                offset,
                dto.getSize()
        );
        Long total = pointChangeLogMapper.countByConditions(
                teamId,
                dto.getChangeType(),
                dto.getSourceType(),
                dto.getSourceId(),
                dto.getOperatorKeyword(),
                dto.getStartTime(),
                dto.getEndTime()
        );
        List<PointChangeLogVO> list = logs.stream().map(this::convertToVO).toList();
        return PageVO.<PointChangeLogVO>builder().list(list).total(total).page(dto.getPage()).size(dto.getSize()).build();
    }

    @Override
    public List<String> listOperatorUsernames(String keyword) {
        Integer teamId = TeamContext.getTeamId();
        return pointChangeLogMapper.selectDistinctOperatorUsernames(teamId, keyword);
    }

    private PointChangeLogVO convertToVO(PointChangeLog logRecord) {
        PointChangeTypeEnum changeType = PointChangeTypeEnum.fromValue(logRecord.getChangeType());
        PointSourceTypeEnum sourceType = PointSourceTypeEnum.fromValue(logRecord.getSourceType());
        LogUserStatusEnum operatorStatus = LogUserStatusEnum.fromValue(logRecord.getOperatorUserStatus());
        return PointChangeLogVO.builder()
                .id(logRecord.getId())
                .changeType(logRecord.getChangeType())
                .changeTypeDesc(changeType != null ? changeType.getDescription() : logRecord.getChangeType())
                .operatorUserId(logRecord.getOperatorUserId())
                .operatorUsername(logRecord.getOperatorUsername())
                .operatorUserStatus(logRecord.getOperatorUserStatus())
                .operatorUserStatusDesc(operatorStatus != null ? operatorStatus.getDescription() : null)
                .sourceType(logRecord.getSourceType())
                .sourceTypeDesc(sourceType != null ? sourceType.getDescription() : logRecord.getSourceType())
                .sourceId(logRecord.getSourceId())
                .points(logRecord.getPoints())
                .balanceBefore(logRecord.getBalanceBefore())
                .balanceAfter(logRecord.getBalanceAfter())
                .description(logRecord.getDescription())
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
