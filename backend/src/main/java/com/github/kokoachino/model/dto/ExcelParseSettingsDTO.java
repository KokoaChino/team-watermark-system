package com.github.kokoachino.model.dto;

import com.github.kokoachino.common.enums.DuplicateHandlingEnum;
import com.github.kokoachino.common.enums.InvalidCharHandlingEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * Excel解析设置 DTO
 *
 * @author Kokoa_Chino
 * @date 2026-02-17
 */
@Data
@Schema(description = "Excel解析设置")
public class ExcelParseSettingsDTO {

    @Schema(description = "重复ID处理策略：first-保留第一个，last-保留最后一个，error-报错终止", 
            example = "first", defaultValue = "first")
    private String duplicateHandling;

    @Schema(description = "异常字符处理策略：underscore-用下划线替代，error_folder-统一放到ERROR文件夹，error-报错终止", 
            example = "underscore", defaultValue = "underscore")
    private String invalidCharHandling;

    public DuplicateHandlingEnum getDuplicateHandlingEnum() {
        return DuplicateHandlingEnum.fromValue(this.duplicateHandling);
    }

    public InvalidCharHandlingEnum getInvalidCharHandlingEnum() {
        return InvalidCharHandlingEnum.fromValue(this.invalidCharHandling);
    }
}
