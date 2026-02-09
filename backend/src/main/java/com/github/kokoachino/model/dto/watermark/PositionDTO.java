package com.github.kokoachino.model.dto.watermark;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 位置配置 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Schema(description = "位置配置")
public class PositionDTO {

    @NotNull(message = "X坐标不能为空")
    @Schema(description = "X坐标（像素）", example = "100")
    private Integer x;

    @NotNull(message = "Y坐标不能为空")
    @Schema(description = "Y坐标（像素）", example = "100")
    private Integer y;
}
