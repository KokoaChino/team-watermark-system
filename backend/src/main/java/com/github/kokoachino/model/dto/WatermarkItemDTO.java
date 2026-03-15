package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 水印项 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-03-02
 */
@Data
@Schema(description = "水印项配置")
public class WatermarkItemDTO {

    @NotBlank(message = "水印ID不能为空")
    @Schema(description = "水印唯一标识（前端生成）", example = "wm_1234567890_abc123")
    private String id;

    @NotBlank(message = "水印类型不能为空")
    @Schema(description = "水印类型", example = "text", allowableValues = {"text", "image"})
    private String type;

    @Schema(description = "水印名称", example = "文字水印")
    private String name;

    @NotNull(message = "X坐标不能为空")
    @Schema(description = "X坐标（像素）", example = "100.0")
    private Double x;

    @NotNull(message = "Y坐标不能为空")
    @Schema(description = "Y坐标（像素）", example = "100.0")
    private Double y;

    @Min(value = -360, message = "旋转角度最小为-360")
    @Max(value = 360, message = "旋转角度最大为360")
    @Schema(description = "旋转角度（度，图片水印使用）", example = "0")
    private Double rotation;

    @Min(value = 0, message = "透明度最小为0")
    @Max(value = 1, message = "透明度最大为1")
    @Schema(description = "透明度（0-1，图片水印使用）", example = "1.0")
    private Double opacity;

    @Schema(description = "文字水印配置（type=text时必填）")
    @Valid
    private TextWatermarkConfigDTO textConfig;

    @Schema(description = "图片水印配置（type=image时必填）")
    @Valid
    private ImageWatermarkConfigDTO imageConfig;
}
