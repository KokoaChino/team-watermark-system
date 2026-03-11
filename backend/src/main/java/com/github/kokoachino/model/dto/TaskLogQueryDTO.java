package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 批量任务日志查询 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@Schema(description = "批量任务日志查询条件")
public class TaskLogQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;

    @Schema(description = "任务状态", example = "completed")
    private String status;

    @Schema(description = "操作人用户名关键字", example = "kokoa")
    private String operatorKeyword;

    @Schema(description = "模板名称关键字", example = "默认模板")
    private String templateName;

    @Schema(description = "任务编号关键字", example = "TSK20260311")
    private String taskNo;

    @Schema(description = "开始时间", example = "2026-03-09T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2026-03-09T23:59:59")
    private LocalDateTime endTime;
}
