package com.github.kokoachino.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kokoachino.common.enums.EventTypeEnum;
import com.github.kokoachino.common.util.UserContext;
import com.github.kokoachino.mapper.OperationLogMapper;
import com.github.kokoachino.model.dto.OperationLogQueryDTO;
import com.github.kokoachino.model.entity.OperationLog;
import com.github.kokoachino.model.vo.OperationLogVO;
import com.github.kokoachino.model.vo.PageVO;
import com.github.kokoachino.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 操作事件日志服务实现
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void log(EventTypeEnum eventTypeEnum, Integer teamId, Integer userId, String username,
                    Integer targetId, String targetName, Object beforeData, Object afterData, Map<String, Object> details) {
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setEventType(eventTypeEnum.getCode());
            operationLog.setTeamId(teamId);
            operationLog.setUserId(userId);
            operationLog.setUsername(username);
            operationLog.setTargetId(targetId);
            operationLog.setTargetName(targetName);
            if (beforeData != null) {
                operationLog.setBeforeData(objectMapper.writeValueAsString(beforeData));
            }
            if (afterData != null) {
                operationLog.setAfterData(objectMapper.writeValueAsString(afterData));
            }
            if (details != null) {
                operationLog.setDetails(objectMapper.writeValueAsString(details));
            }
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                operationLog.setIpAddress(getClientIp(request));
            }
            operationLog.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    @Override
    public void log(EventTypeEnum eventTypeEnum, Integer targetId, String targetName, Map<String, Object> details) {
        try {
            Integer userId = UserContext.getUserId();
            String username = UserContext.getUser().getUsername();
            Integer teamId = UserContext.getUser().getTeamId();
            log(eventTypeEnum, teamId, userId, username, targetId, targetName, null, null, details);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    @Override
    public PageVO<OperationLogVO> queryTeamLogs(OperationLogQueryDTO dto) {
        Integer teamId = UserContext.getUser().getTeamId();
        Integer offset = (dto.getPage() - 1) * dto.getSize();
        List<OperationLog> logs = operationLogMapper.selectByConditions(
                teamId,
                dto.getEventType(),
                dto.getUserId(),
                dto.getStartTime(),
                dto.getEndTime(),
                offset,
                dto.getSize()
        );
        Long total = operationLogMapper.countByConditions(
                teamId,
                dto.getEventType(),
                dto.getUserId(),
                dto.getStartTime(),
                dto.getEndTime()
        );
        List<OperationLogVO> voList = logs.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return PageVO.<OperationLogVO>builder()
                .list(voList)
                .total(total)
                .page(dto.getPage())
                .size(dto.getSize())
                .build();
    }

    private OperationLogVO convertToVO(OperationLog log) {
        return OperationLogVO.builder()
                .id(log.getId())
                .eventType(log.getEventType())
                .eventTypeDesc(EventTypeEnum.valueOf(log.getEventType()).getDescription())
                .category(EventTypeEnum.valueOf(log.getEventType()).getCategory())
                .teamId(log.getTeamId())
                .userId(log.getUserId())
                .username(log.getUsername())
                .targetId(log.getTargetId())
                .targetName(log.getTargetName())
                .beforeData(log.getBeforeData())
                .afterData(log.getAfterData())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
