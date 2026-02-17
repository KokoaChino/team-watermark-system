package com.github.kokoachino.common.enums;

import lombok.Getter;


/**
 * Excel映射模式枚举
 *
 * @author Kokoa_Chino
 * @date 2026-02-17
 */
@Getter
public enum MappingModeEnum {

    ID("id", "图片ID映射"),
    ORDER("order", "按顺序映射");

    private final String value;
    private final String description;

    MappingModeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static MappingModeEnum fromValue(String value) {
        if (value == null) {
            return ID;
        }
        for (MappingModeEnum mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return ID;
    }
}
