package com.github.kokoachino.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author kokoachino
 * @date 2026-02-02
 */
@Getter
@AllArgsConstructor
public enum BlackListTypeEnum {

    TOKEN("token", "JWT黑名单"),
    EMAIL("email", "邮箱黑名单");

    private final String value;
    private final String desc;

    public static BlackListTypeEnum fromValue(String value) {
        for (BlackListTypeEnum type : BlackListTypeEnum.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
