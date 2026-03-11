package com.github.kokoachino.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kokoachino.common.enums.LogUserStatusEnum;
import com.github.kokoachino.common.enums.WatermarkResourceEventTypeEnum;
import com.github.kokoachino.common.enums.WatermarkResourceScopeEnum;
import com.github.kokoachino.common.util.RequestIpUtils;
import com.github.kokoachino.common.util.TeamContext;
import com.github.kokoachino.mapper.WatermarkResourceLogMapper;
import com.github.kokoachino.model.dto.WatermarkResourceLogQueryDTO;
import com.github.kokoachino.model.dto.WatermarkResourceLogRecordDTO;
import com.github.kokoachino.model.entity.WatermarkResourceLog;
import com.github.kokoachino.model.vo.PageVO;
import com.github.kokoachino.model.vo.WatermarkResourceLogVO;
import com.github.kokoachino.service.WatermarkResourceLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 水印资源日志服务实现
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatermarkResourceLogServiceImpl implements WatermarkResourceLogService {

    private final WatermarkResourceLogMapper watermarkResourceLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void record(WatermarkResourceLogRecordDTO dto) {
        if (dto == null || dto.getTeamId() == null || dto.getEventType() == null || dto.getResourceScope() == null) {
            return;
        }
        try {
            WatermarkResourceLog logRecord = new WatermarkResourceLog();
            logRecord.setTeamId(dto.getTeamId());
            logRecord.setResourceScope(dto.getResourceScope().getValue());
            logRecord.setEventType(dto.getEventType().getValue());
            logRecord.setOperatorUserId(dto.getOperatorUserId());
            logRecord.setOperatorUsername(dto.getOperatorUsername());
            logRecord.setOperatorUserStatus(dto.getOperatorUserStatus());
            logRecord.setResourceId(dto.getResourceId());
            logRecord.setResourceName(dto.getResourceName());
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
            watermarkResourceLogMapper.insert(logRecord);
        } catch (Exception e) {
            log.error("记录水印资源日志失败", e);
        }
    }

    @Override
    public PageVO<WatermarkResourceLogVO> queryLogs(WatermarkResourceLogQueryDTO dto) {
        Integer teamId = TeamContext.getTeamId();
        Integer offset = (dto.getPage() - 1) * dto.getSize();
        List<WatermarkResourceLog> logs = watermarkResourceLogMapper.selectByConditions(
                teamId,
                dto.getEventType(),
                dto.getResourceScope(),
                dto.getResourceName(),
                dto.getOperatorKeyword(),
                dto.getStartTime(),
                dto.getEndTime(),
                offset,
                dto.getSize()
        );
        Long total = watermarkResourceLogMapper.countByConditions(
                teamId,
                dto.getEventType(),
                dto.getResourceScope(),
                dto.getResourceName(),
                dto.getOperatorKeyword(),
                dto.getStartTime(),
                dto.getEndTime()
        );
        List<WatermarkResourceLogVO> list = logs.stream().map(this::convertToVO).toList();
        return PageVO.<WatermarkResourceLogVO>builder().list(list).total(total).page(dto.getPage()).size(dto.getSize()).build();
    }

    @Override
    public List<String> listOperatorUsernames(String keyword) {
        Integer teamId = TeamContext.getTeamId();
        return watermarkResourceLogMapper.selectDistinctOperatorUsernames(teamId, keyword);
    }

    private WatermarkResourceLogVO convertToVO(WatermarkResourceLog logRecord) {
        WatermarkResourceScopeEnum resourceScope = WatermarkResourceScopeEnum.fromValue(logRecord.getResourceScope());
        WatermarkResourceEventTypeEnum eventType = WatermarkResourceEventTypeEnum.fromValue(logRecord.getEventType());
        LogUserStatusEnum operatorStatus = LogUserStatusEnum.fromValue(logRecord.getOperatorUserStatus());
        return WatermarkResourceLogVO.builder()
                .id(logRecord.getId())
                .resourceScope(logRecord.getResourceScope())
                .resourceScopeDesc(resourceScope != null ? resourceScope.getDescription() : logRecord.getResourceScope())
                .eventType(logRecord.getEventType())
                .eventTypeDesc(eventType != null ? eventType.getDescription() : logRecord.getEventType())
                .operatorUserId(logRecord.getOperatorUserId())
                .operatorUsername(logRecord.getOperatorUsername())
                .operatorUserStatus(logRecord.getOperatorUserStatus())
                .operatorUserStatusDesc(operatorStatus != null ? operatorStatus.getDescription() : null)
                .resourceId(logRecord.getResourceId())
                .resourceName(logRecord.getResourceName())
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
