package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * Excel基座模板下载请求 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-03-14
 */
@Data
@Schema(description = "Excel基座模板下载请求")
public class ExcelTemplateBaseRequestDTO {

    @NotBlank(message = "映射模式不能为空")
    @Schema(description = "映射模式：id-按图片ID映射，order-按顺序映射", example = "id")
    private String mappingMode;

    @NotNull(message = "文字水印数量不能为空")
    @Min(value = 0, message = "文字水印数量不能小于0")
    @Schema(description = "模板中的文字水印数量", example = "2")
    private Integer textWatermarkCount;

    @NotNull(message = "图片水印数量不能为空")
    @Min(value = 0, message = "图片水印数量不能小于0")
    @Schema(description = "模板中的图片水印数量", example = "1")
    private Integer imageWatermarkCount;
}
