package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 团队变更日志 VO
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@Builder
@Schema(description = "团队变更日志")
public class TeamEventLogVO {

    private Integer id;
    private String eventType;
    private String eventTypeDesc;
    private Integer operatorUserId;
    private String operatorUsername;
    private String operatorUserStatus;
    private String operatorUserStatusDesc;
    private Integer affectedUserId;
    private String affectedUsername;
    private String affectedUserStatus;
    private String affectedUserStatusDesc;
    private Integer inviteCodeId;
    private String inviteCode;
    private String beforeData;
    private String afterData;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
