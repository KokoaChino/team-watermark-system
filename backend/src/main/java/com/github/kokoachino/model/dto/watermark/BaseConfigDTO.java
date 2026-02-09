package com.github.kokoachino.model.dto.watermark;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 底图配置 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Schema(description = "底图配置")
public class BaseConfigDTO {

    @NotNull(message = "宽度不能为空")
    @Min(value = 100, message = "宽度最小为100")
    @Max(value = 5000, message = "宽度最大为5000")
    @Schema(description = "画布宽度（像素）", example = "800")
    private Integer width;

    @NotNull(message = "高度不能为空")
    @Min(value = 100, message = "高度最小为100")
    @Max(value = 5000, message = "高度最大为5000")
    @Schema(description = "画布高度（像素）", example = "600")
    private Integer height;

    @Schema(description = "背景颜色（十六进制）", example = "#ffffff")
    private String backgroundColor;
}
