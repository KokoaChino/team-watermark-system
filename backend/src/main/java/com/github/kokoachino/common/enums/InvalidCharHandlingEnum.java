package com.github.kokoachino.common.enums;

import lombok.Getter;


/**
 * 异常字符处理策略枚举
 *
 * @author Kokoa_Chino
 * @date 2026-02-17
 */
@Getter
public enum InvalidCharHandlingEnum {

    UNDERSCORE("underscore", "用下划线替代"),
    ERROR_FOLDER("error_folder", "统一放到ERROR文件夹"),
    ERROR("error", "报错终止");

    private final String value;
    private final String description;

    InvalidCharHandlingEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static InvalidCharHandlingEnum fromValue(String value) {
        if (value == null) {
            return UNDERSCORE;
        }
        for (InvalidCharHandlingEnum strategy : values()) {
            if (strategy.value.equalsIgnoreCase(value)) {
                return strategy;
            }
        }
        return UNDERSCORE;
    }
}
