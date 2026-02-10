package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 保存草稿 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Data
@Schema(description = "保存草稿请求")
public class SaveDraftDTO {

    @Schema(description = "源模板ID（为空表示新建）", example = "1")
    private Integer sourceTemplateId;

    @Schema(description = "源模板版本号", example = "1")
    private Integer sourceVersion;

    @Schema(description = "草稿名称", example = "我的草稿")
    private String name;

    @NotNull(message = "水印配置不能为空")
    @Valid
    @Schema(description = "水印配置")
    private WatermarkConfigDTO config;
}
