package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;


/**
 * 渐变配置 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Schema(description = "渐变配置")
public class GradientDTO {

    @Schema(description = "渐变类型", example = "linear", allowableValues = {"linear", "radial"})
    private String type;

    @Schema(description = "渐变角度（线性渐变时使用，度）", example = "90")
    private Integer angle;

    @Schema(description = "渐变颜色停止点")
    private List<GradientStopDTO> stops;

    /**
     * 渐变颜色停止点
     */
    @Data
    @Schema(description = "渐变颜色停止点")
    public static class GradientStopDTO {

        @Schema(description = "位置（0-1）", example = "0")
        private Double offset;

        @Schema(description = "颜色（十六进制）", example = "#ff0000")
        private String color;
    }
}
