package com.github.kokoachino.common.enums;

import lombok.Getter;


/**
 * 重复ID处理策略枚举
 *
 * @author Kokoa_Chino
 * @date 2026-02-17
 */
@Getter
public enum DuplicateHandlingEnum {

    FIRST("first", "保留第一个"),
    LAST("last", "保留最后一个"),
    ERROR("error", "报错终止");

    private final String value;
    private final String description;

    DuplicateHandlingEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static DuplicateHandlingEnum fromValue(String value) {
        if (value == null) {
            return FIRST;
        }
        for (DuplicateHandlingEnum strategy : values()) {
            if (strategy.value.equalsIgnoreCase(value)) {
                return strategy;
            }
        }
        return FIRST;
    }
}
