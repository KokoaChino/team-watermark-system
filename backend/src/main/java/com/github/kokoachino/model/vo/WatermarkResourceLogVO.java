package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 水印资源日志 VO
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@Builder
@Schema(description = "水印资源日志")
public class WatermarkResourceLogVO {

    private Integer id;
    private String resourceScope;
    private String resourceScopeDesc;
    private String eventType;
    private String eventTypeDesc;
    private Integer operatorUserId;
    private String operatorUsername;
    private String operatorUserStatus;
    private String operatorUserStatusDesc;
    private Integer resourceId;
    private String resourceName;
    private String beforeData;
    private String afterData;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
