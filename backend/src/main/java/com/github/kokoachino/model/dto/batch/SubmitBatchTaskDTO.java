package com.github.kokoachino.model.dto.batch;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 提交批量任务 DTO（简化版）
 * 前端负责执行任务，后端只负责预扣点数
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Data
@Schema(description = "提交批量任务请求")
public class SubmitBatchTaskDTO {

    @NotNull(message = "图片数量不能为空")
    @Min(value = 1, message = "至少需要1张图片")
    @Max(value = 1000, message = "单次最多1000张图片")
    @Schema(description = "图片数量（用于预扣点数）", example = "50")
    private Integer imageCount;

    @Schema(description = "任务描述（可选）", example = "商品主图批量加水印")
    private String description;
}
