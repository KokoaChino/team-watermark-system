package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 操作日志查询 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@Schema(description = "操作日志查询条件")
public class OperationLogQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;

    @Schema(description = "事件类型", example = "TEMPLATE_CREATE")
    private String eventType;

    @Schema(description = "操作用户 ID", example = "1")
    private Integer userId;

    @Schema(description = "开始时间", example = "2026-02-10T14:30:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2026-02-11T14:30:00")
    private LocalDateTime endTime;
}
