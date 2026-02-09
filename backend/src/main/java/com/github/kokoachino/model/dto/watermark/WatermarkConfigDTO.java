package com.github.kokoachino.model.dto.watermark;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;


/**
 * 水印模板完整配置 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Schema(description = "水印模板完整配置")
public class WatermarkConfigDTO {

    @NotNull(message = "底图配置不能为空")
    @Valid
    @Schema(description = "底图配置")
    private BaseConfigDTO baseConfig;

    @Schema(description = "水印列表（按顺序从后往前渲染）")
    private List<@Valid WatermarkBaseDTO> watermarks;
}
