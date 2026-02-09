package com.github.kokoachino.model.dto.watermark;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 文字水印配置 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文字水印配置")
public class TextWatermarkDTO extends WatermarkBaseDTO {

    @NotBlank(message = "文字内容不能为空")
    @Schema(description = "文字内容", example = "版权所有")
    private String content;

    @NotBlank(message = "字体不能为空")
    @Schema(description = "字体名称", example = "Arial")
    private String fontFamily;

    @NotNull(message = "字体大小不能为空")
    @Schema(description = "字体大小（像素）", example = "24")
    private Integer fontSize;

    @Schema(description = "字体颜色（十六进制）", example = "#000000")
    private String color;

    @Schema(description = "是否粗体", example = "false")
    private Boolean bold;

    @Schema(description = "是否斜体", example = "false")
    private Boolean italic;

    @Schema(description = "描边配置")
    private StrokeDTO stroke;

    @Schema(description = "阴影配置")
    private ShadowDTO shadow;

    @Schema(description = "渐变配置（优先级高于单色）")
    private GradientDTO gradient;
}
