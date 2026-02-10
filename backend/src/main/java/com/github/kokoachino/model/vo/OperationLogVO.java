package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 操作日志 VO
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@Builder
@Schema(description = "操作日志信息")
public class OperationLogVO {

    @Schema(description = "日志ID", example = "1")
    private Integer id;

    @Schema(description = "事件类型代码", example = "TEMPLATE_CREATE")
    private String eventType;

    @Schema(description = "事件类型描述", example = "模板创建")
    private String eventTypeDesc;

    @Schema(description = "事件分类", example = "水印模板")
    private String category;

    @Schema(description = "团队ID", example = "1")
    private Integer teamId;

    @Schema(description = "操作用户ID", example = "2")
    private Integer userId;

    @Schema(description = "操作用户名（记录时的用户名）", example = "zhangsan")
    private String username;

    @Schema(description = "用户当前状态：active-正常，renamed-已改名，left-已离队，deleted-已注销", example = "active")
    private String userStatus;

    @Schema(description = "目标ID", example = "10")
    private Integer targetId;

    @Schema(description = "目标名称", example = "商品主图水印")
    private String targetName;

    @Schema(description = "操作前数据（JSON）")
    private String beforeData;

    @Schema(description = "操作后数据（JSON）")
    private String afterData;

    @Schema(description = "详情（JSON）")
    private String details;

    @Schema(description = "IP地址", example = "192.168.1.100")
    private String ipAddress;

    @Schema(description = "创建时间", example = "2026-02-10T14:30:00")
    private LocalDateTime createdAt;
}
