package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 点数流水日志 VO
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@Builder
@Schema(description = "点数流水日志")
public class PointChangeLogVO {

    private Integer id;
    private String changeType;
    private String changeTypeDesc;
    private Integer operatorUserId;
    private String operatorUsername;
    private String operatorUserStatus;
    private String operatorUserStatusDesc;
    private String sourceType;
    private String sourceTypeDesc;
    private String sourceId;
    private Integer points;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private String description;
    private String ipAddress;
    private LocalDateTime createdAt;
}
