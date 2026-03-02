package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;


/**
 * 文字水印配置 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-03-02
 */
@Data
@Schema(description = "文字水印配置")
public class TextWatermarkConfigDTO {

    @NotBlank(message = "文字内容不能为空")
    @Schema(description = "文字内容", example = "版权所有")
    private String content;

    @NotNull(message = "字体大小不能为空")
    @Min(value = 1, message = "字体大小最小为1")
    @Max(value = 10000, message = "字体大小最大为10000")
    @Schema(description = "字体大小（像素）", example = "32")
    private Integer fontSize;

    @NotBlank(message = "字体名称不能为空")
    @Schema(description = "字体名称", example = "Microsoft YaHei")
    private String fontFamily;

    @Schema(description = "字体文件URL（自定义字体时使用）", example = "https://minio.example.com/fonts/custom.ttf")
    private String fontUrl;

    @NotBlank(message = "文字颜色不能为空")
    @Schema(description = "文字颜色（十六进制）", example = "#333333")
    private String color;

    @NotBlank(message = "对齐方式不能为空")
    @Schema(description = "对齐方式", example = "left", allowableValues = {"left", "center", "right"})
    private String align;

    @NotNull(message = "字重不能为空")
    @Min(value = 100, message = "字重最小为100")
    @Max(value = 900, message = "字重最大为900")
    @Schema(description = "字重（100-900）", example = "400")
    private Integer fontWeight;

    @Schema(description = "倾斜角度（度），0表示正常", example = "0")
    private Double italicAngle;

    @Schema(description = "旋转角度（度）", example = "0")
    private Double rotation;

    @Min(value = 0, message = "透明度最小为0")
    @Max(value = 1, message = "透明度最大为1")
    @Schema(description = "透明度（0-1）", example = "1.0")
    private Double opacity;

    @Schema(description = "字符间距（像素，正值为加宽，负值为紧缩）", example = "0")
    private Double letterSpacing;

    @NotNull(message = "描边启用状态不能为空")
    @Schema(description = "是否启用描边", example = "false")
    private Boolean strokeEnabled;

    @Schema(description = "描边颜色（十六进制）", example = "#000000")
    private String strokeColor;

    @Schema(description = "描边宽度（像素）", example = "0")
    private Integer strokeWidth;

    @NotNull(message = "阴影启用状态不能为空")
    @Schema(description = "是否启用阴影", example = "false")
    private Boolean shadowEnabled;

    @Schema(description = "阴影颜色（十六进制）", example = "#000000")
    private String shadowColor;

    @Schema(description = "阴影模糊半径（像素）", example = "0")
    private Integer shadowBlur;

    @Schema(description = "阴影X轴偏移（像素）", example = "0")
    private Integer shadowOffsetX;

    @Schema(description = "阴影Y轴偏移（像素）", example = "0")
    private Integer shadowOffsetY;

    @NotNull(message = "渐变启用状态不能为空")
    @Schema(description = "是否启用渐变", example = "false")
    private Boolean gradientEnabled;

    @Schema(description = "渐变颜色列表（至少两个颜色）", example = "[\"#ff0000\", \"#00ff00\"]")
    private List<String> gradientColors;

    @Schema(description = "渐变角度（度）", example = "0")
    private Double gradientAngle;
}
