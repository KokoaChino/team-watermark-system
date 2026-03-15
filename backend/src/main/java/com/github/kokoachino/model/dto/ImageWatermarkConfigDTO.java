package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 图片水印配置 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-03-02
 */
@Data
@Schema(description = "图片水印配置")
public class ImageWatermarkConfigDTO {

    @NotBlank(message = "图片URL不能为空")
    @Schema(description = "图片URL（MinIO访问地址）", example = "https://minio.example.com/watermarks/logo.png")
    private String imageUrl;

    @Schema(description = "图片在MinIO中的Key", example = "watermarks/logo.png")
    private String imageKey;

    @NotNull(message = "缩放比例不能为空")
    @Schema(description = "缩放比例（百分比，100为原大小）", example = "100")
    private Integer scale;

    @NotNull(message = "透明度不能为空")
    @Min(value = 0, message = "透明度最小为0")
    @Max(value = 1, message = "透明度最大为1")
    @Schema(description = "透明度（0-1，1为完全不透明）", example = "1")
    private Double opacity;

    @NotBlank(message = "适应模式不能为空")
    @Schema(description = "适应模式", example = "aspectFit", 
            allowableValues = {"none", "scaleToFill", "aspectFit", "aspectFill"})
    private String fitMode;

    @NotBlank(message = "锚点位置不能为空")
    @Schema(description = "锚点位置", example = "none",
            allowableValues = {"none", "topLeft", "topRight", "bottomLeft", "bottomRight", "center"})
    private String anchor;

    @Schema(description = "原始宽度（像素，用于计算）", example = "200")
    private Integer originalWidth;

    @Schema(description = "原始高度（像素，用于计算）", example = "100")
    private Integer originalHeight;
}
