package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 水印资源日志查询 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@Schema(description = "水印资源日志查询条件")
public class WatermarkResourceLogQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;

    @Schema(description = "事件类型", example = "template_create")
    private String eventType;

    @Schema(description = "资源范围", example = "template")
    private String resourceScope;

    @Schema(description = "资源名称关键字", example = "示例模板")
    private String resourceName;

    @Schema(description = "操作人用户名关键字", example = "kokoa")
    private String operatorKeyword;

    @Schema(description = "开始时间", example = "2026-03-09T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2026-03-09T23:59:59")
    private LocalDateTime endTime;
}
