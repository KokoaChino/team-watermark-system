package com.github.kokoachino.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 字体查询 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-16
 */
@Data
@Schema(description = "字体查询条件")
public class FontQueryDTO {

    @Schema(description = "字体名称（模糊匹配）", example = "宋体")
    private String name;

    @Schema(description = "是否只查询系统字体", example = "true")
    private Boolean systemFontOnly;

    @Schema(description = "是否只查询团队上传的字体", example = "false")
    private Boolean teamFontOnly;
}
