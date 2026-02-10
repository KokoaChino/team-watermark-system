package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 批量任务 VO（简化版）
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@Builder
@Schema(description = "批量任务信息")
public class BatchTaskVO {

    @Schema(description = "任务ID")
    private Integer id;

    @Schema(description = "任务编号")
    private String taskNo;

    @Schema(description = "预扣点数")
    private Integer deductedPoints;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
