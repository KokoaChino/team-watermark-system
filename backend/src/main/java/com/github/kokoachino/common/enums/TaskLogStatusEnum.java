package com.github.kokoachino.common.enums;

import lombok.Getter;


/**
 * 任务日志状态枚举
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Getter
public enum TaskLogStatusEnum {

    SUBMITTED("submitted", "已提交"),
    COMPLETED("completed", "已完成");

    private final String value;
    private final String description;

    TaskLogStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static TaskLogStatusEnum fromValue(String value) {
        for (TaskLogStatusEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
