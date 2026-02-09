package com.github.kokoachino.model.dto.watermark;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 描边配置 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Schema(description = "描边配置")
public class StrokeDTO {

    @Schema(description = "描边宽度（像素）", example = "0")
    private Integer width;

    @Schema(description = "描边颜色（十六进制）", example = "#000000")
    private String color;
}
