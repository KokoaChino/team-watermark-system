package com.github.kokoachino.service;

import com.github.kokoachino.model.dto.log.OperationLogQueryDTO;
import com.github.kokoachino.model.enums.EventType;
import com.github.kokoachino.model.vo.OperationLogVO;
import com.github.kokoachino.model.vo.PageVO;
import java.util.Map;


/**
 * 操作事件日志服务接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
public interface OperationLogService {

    /**
     * 记录操作日志
     *
     * @param eventType   事件类型
     * @param teamId      团队ID
     * @param userId      用户ID
     * @param username    用户名
     * @param targetId    目标ID
     * @param targetName  目标名称
     * @param beforeData  操作前数据
     * @param afterData   操作后数据
     * @param details     详情
     */
    void log(EventType eventType, Integer teamId, Integer userId, String username,
             Integer targetId, String targetName, Object beforeData, Object afterData, Map<String, Object> details);

    /**
     * 简化的日志记录方法
     *
     * @param eventType  事件类型
     * @param targetId   目标ID
     * @param targetName 目标名称
     * @param details    详情
     */
    void log(EventType eventType, Integer targetId, String targetName, Map<String, Object> details);

    /**
     * 查询团队日志（分页）
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    PageVO<OperationLogVO> queryTeamLogs(OperationLogQueryDTO dto);
}
