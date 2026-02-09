package com.github.kokoachino.model.dto;

import com.github.kokoachino.model.dto.watermark.WatermarkConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 创建模板 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Schema(description = "创建水印模板请求")
public class CreateTemplateDTO {

    @NotBlank(message = "模板名称不能为空")
    @Schema(description = "模板名称", example = "我的水印模板")
    private String name;

    @NotNull(message = "水印配置不能为空")
    @Valid
    @Schema(description = "水印配置")
    private WatermarkConfigDTO config;
}
