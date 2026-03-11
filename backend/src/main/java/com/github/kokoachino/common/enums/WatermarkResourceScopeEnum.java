package com.github.kokoachino.common.enums;

import lombok.Getter;


/**
 * 水印资源范围枚举
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Getter
public enum WatermarkResourceScopeEnum {

    TEMPLATE("template", "模板"),
    FONT("font", "字体");

    private final String value;
    private final String description;

    WatermarkResourceScopeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static WatermarkResourceScopeEnum fromValue(String value) {
        for (WatermarkResourceScopeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
