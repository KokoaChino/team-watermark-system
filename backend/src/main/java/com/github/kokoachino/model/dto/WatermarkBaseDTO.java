package com.github.kokoachino.model.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 水印基础配置 DTO
 * 使用 Jackson 多态序列化支持文字和图片水印
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextWatermarkDTO.class, name = "text"),
        @JsonSubTypes.Type(value = ImageWatermarkDTO.class, name = "image")
})
@Schema(description = "水印基础配置", subTypes = {TextWatermarkDTO.class, ImageWatermarkDTO.class})
public abstract class WatermarkBaseDTO {

    @NotBlank(message = "水印类型不能为空")
    @Schema(description = "水印类型", example = "text", allowableValues = {"text", "image"})
    private String type;

    @NotNull(message = "位置不能为空")
    @Schema(description = "位置配置")
    private PositionDTO position;

    @Min(value = -360, message = "旋转角度最小为-360")
    @Max(value = 360, message = "旋转角度最大为360")
    @Schema(description = "旋转角度（度）", example = "0")
    private Integer rotation;

    @Min(value = 0, message = "透明度最小为0")
    @Max(value = 1, message = "透明度最大为1")
    @Schema(description = "透明度（0-1）", example = "1.0")
    private Double opacity;
}
