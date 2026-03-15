package com.github.kokoachino.common.enums;

import lombok.Getter;


/**
 * 点数变动类型枚举
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Getter
public enum PointChangeTypeEnum {

    RECHARGE("recharge", "充值"),
    DEDUCT("deduct", "预扣"),
    REFUND("refund", "返还");

    private final String value;
    private final String description;

    PointChangeTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static PointChangeTypeEnum fromValue(String value) {
        for (PointChangeTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
