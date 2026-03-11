package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;


/**
 * 点数流水日志查询 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@Schema(description = "点数流水日志查询条件")
public class PointChangeLogQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;

    @Schema(description = "变动类型", example = "recharge")
    private String changeType;

    @Schema(description = "来源业务类型", example = "batch_task")
    private String sourceType;

    @Schema(description = "来源标识关键字", example = "TSK123")
    private String sourceId;

    @Schema(description = "操作人用户名关键字", example = "zhangsan")
    private String operatorKeyword;

    @Schema(description = "开始时间", example = "2026-03-09T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2026-03-09T23:59:59")
    private LocalDateTime endTime;
}
