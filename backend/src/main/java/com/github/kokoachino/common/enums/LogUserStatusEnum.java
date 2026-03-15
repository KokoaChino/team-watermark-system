package com.github.kokoachino.common.enums;

import lombok.Getter;


/**
 * 日志中的用户状态枚举
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
@Getter
public enum LogUserStatusEnum {

    ACTIVE("active", "正常"),
    RENAMED("renamed", "已改名"),
    LEFT("left", "已离队"),
    DELETED("deleted", "已注销");

    private final String value;
    private final String description;

    LogUserStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static LogUserStatusEnum fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (LogUserStatusEnum status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
}
