package com.github.kokoachino.common.enums;

import lombok.Getter;


/**
 * 点数流水来源类型枚举
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Getter
public enum PointSourceTypeEnum {

    PAYMENT("payment", "支付订单"),
    BATCH_TASK("batch_task", "批量任务");

    private final String value;
    private final String description;

    PointSourceTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static PointSourceTypeEnum fromValue(String value) {
        for (PointSourceTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
