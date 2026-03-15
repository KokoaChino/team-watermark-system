package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 提交批量任务 DTO
 * 前端负责执行任务，后端负责记录任务日志与点数结算
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Data
@Schema(description = "提交批量任务请求")
public class SubmitBatchTaskDTO {

    @NotNull(message = "任务总数量不能为空")
    @Min(value = 1, message = "至少需要1张图片")
    @Max(value = 1000, message = "单次最多1000张图片")
    @Schema(description = "任务总数量", example = "50")
    private Integer totalCount;

    @NotNull(message = "任务总大小不能为空")
    @Min(value = 0, message = "任务总大小不能为负数")
    @Schema(description = "任务总大小（字节）", example = "10485760")
    private Long totalSize;

    @NotNull(message = "模板ID不能为空")
    @Schema(description = "模板ID", example = "1")
    private Integer templateId;

    @NotBlank(message = "模板名称不能为空")
    @Schema(description = "模板名称", example = "商品主图模板")
    private String templateName;

    @NotNull(message = "模板版本不能为空")
    @Schema(description = "模板版本", example = "3")
    private Integer templateVersion;

    @NotNull(message = "模板快照不能为空")
    @Schema(description = "模板快照")
    private Object templateSnapshot;

    @Schema(description = "任务描述（可选）", example = "商品主图批量加水印")
    private String description;
}
