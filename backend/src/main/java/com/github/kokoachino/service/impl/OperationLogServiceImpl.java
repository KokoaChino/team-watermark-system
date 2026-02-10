package com.github.kokoachino.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kokoachino.common.util.UserContext;
import com.github.kokoachino.mapper.OperationLogMapper;
import com.github.kokoachino.model.dto.log.OperationLogQueryDTO;
import com.github.kokoachino.model.entity.OperationLog;
import com.github.kokoachino.model.enums.EventType;
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
    public void log(EventType eventType, Integer teamId, Integer userId, String username,
                    Integer targetId, String targetName, Object beforeData, Object afterData, Map<String, Object> details) {
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setEventType(eventType.getCode());
            operationLog.setTeamId(teamId);
            operationLog.setUserId(userId);
            operationLog.setUsername(username);
            operationLog.setTargetId(targetId);
            operationLog.setTargetName(targetName);
            // 序列化数据
            if (beforeData != null) {
                operationLog.setBeforeData(objectMapper.writeValueAsString(beforeData));
            }
            if (afterData != null) {
                operationLog.setAfterData(objectMapper.writeValueAsString(afterData));
            }
            if (details != null) {
                operationLog.setDetails(objectMapper.writeValueAsString(details));
            }
            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                operationLog.setIpAddress(getClientIp(request));
                operationLog.setUserAgent(request.getHeader("User-Agent"));
            }
            operationLog.setCreatedAt(LocalDateTime.now());
            operationLog.setCreatedBy(username);
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    @Override
    public void log(EventType eventType, Integer targetId, String targetName, Map<String, Object> details) {
        try {
            Integer userId = UserContext.getUserId();
            String username = UserContext.getUser().getUsername();
            Integer teamId = UserContext.getUser().getTeamId();
            log(eventType, teamId, userId, username, targetId, targetName, null, null, details);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    @Override
    public PageVO<OperationLogVO> queryTeamLogs(OperationLogQueryDTO dto) {
        Integer teamId = UserContext.getUser().getTeamId();
        Integer offset = (dto.getPage() - 1) * dto.getSize();
        // 查询数据
        List<OperationLog> logs = operationLogMapper.selectByConditions(
                teamId,
                dto.getEventType(),
                dto.getUserId(),
                dto.getStartTime(),
                dto.getEndTime(),
                offset,
                dto.getSize()
        );
        // 统计总数
        Long total = operationLogMapper.countByConditions(
                teamId,
                dto.getEventType(),
                dto.getUserId(),
                dto.getStartTime(),
                dto.getEndTime()
        );
        // 转换为VO
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

    /**
     * 转换为VO
     */
    private OperationLogVO convertToVO(OperationLog log) {
        return OperationLogVO.builder()
                .id(log.getId())
                .eventType(log.getEventType())
                .eventTypeDesc(EventType.valueOf(log.getEventType()).getDescription())
                .category(EventType.valueOf(log.getEventType()).getCategory())
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

    /**
     * 获取客户端IP
     */
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
        // 多个代理情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
