package com.github.kokoachino.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 团队变更日志实体
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@TableName("tw_team_event_log")
public class TeamEventLog {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer teamId;
    private String eventType;
    private Integer operatorUserId;
    private String operatorUsername;
    private String operatorUserStatus;
    private Integer affectedUserId;
    private String affectedUsername;
    private String affectedUserStatus;
    private Integer inviteCodeId;
    private String inviteCode;
    private String beforeData;
    private String afterData;
    private String details;
    private String ipAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
