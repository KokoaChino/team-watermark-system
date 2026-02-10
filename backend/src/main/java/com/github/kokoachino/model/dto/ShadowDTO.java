package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 阴影配置 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Schema(description = "阴影配置")
public class ShadowDTO {

    @Schema(description = "模糊半径（像素）", example = "0")
    private Integer blur;

    @Schema(description = "阴影颜色（十六进制）", example = "#000000")
    private String color;

    @Schema(description = "X轴偏移（像素）", example = "0")
    private Integer offsetX;

    @Schema(description = "Y轴偏移（像素）", example = "0")
    private Integer offsetY;
}
