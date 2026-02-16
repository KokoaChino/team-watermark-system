package com.github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 操作事件日志实体
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@TableName("tw_operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 团队ID
     */
    private Integer teamId;

    /**
     * 操作用户ID
     */
    private Integer userId;

    /**
     * 操作用户名（记录时的用户名）
     */
    private String username;

    /**
     * 用户当前状态：active-正常，renamed-已改名，left-已离队，deleted-已注销
     */
    private String userStatus;

    /**
     * 操作目标ID
     */
    private Integer targetId;

    /**
     * 操作目标名称
     */
    private String targetName;

    /**
     * 操作前数据（JSON）
     */
    private String beforeData;

    /**
     * 操作后数据（JSON）
     */
    private String afterData;

    /**
     * 事件详情（JSON）
     */
    private String details;

    /**
     * 操作 IP 地址
     */
    private String ipAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
