package com.github.kokoachino.model.dto;

import com.github.kokoachino.common.enums.TeamEventTypeEnum;
import lombok.Builder;
import lombok.Data;
import java.util.Map;


/**
 * 团队变更日志写入命令
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@Builder
public class TeamEventLogRecordDTO {

    private Integer teamId;
    private TeamEventTypeEnum eventType;
    private Integer operatorUserId;
    private String operatorUsername;
    private String operatorUserStatus;
    private Integer affectedUserId;
    private String affectedUsername;
    private String affectedUserStatus;
    private Integer inviteCodeId;
    private String inviteCode;
    private Object beforeData;
    private Object afterData;
    private Map<String, Object> details;
}
