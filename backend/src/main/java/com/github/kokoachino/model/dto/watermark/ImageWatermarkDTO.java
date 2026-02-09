package com.github.kokoachino.model.dto.watermark;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 图片水印配置 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "图片水印配置")
public class ImageWatermarkDTO extends WatermarkBaseDTO {

    @NotBlank(message = "图片URL不能为空")
    @Schema(description = "图片URL（MinIO访问地址）", example = "http://minio.example.com/fonts/logo.png")
    private String imageUrl;

    @Schema(description = "缩放比例（1.0为原大小）", example = "1.0")
    private Double scale;

    @Schema(description = "宽度（像素，设置后scale失效）", example = "200")
    private Integer width;

    @Schema(description = "高度（像素，设置后scale失效）", example = "100")
    private Integer height;
}
