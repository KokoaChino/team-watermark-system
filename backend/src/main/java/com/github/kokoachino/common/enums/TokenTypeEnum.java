package com.github.kokoachino.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * Token 类型枚举
 *
 * @author kokoachino
 * @date 2026-02-03
 */
@Getter
@AllArgsConstructor
public enum TokenTypeEnum {

    ACCESS("access", "访问令牌"),
    REFRESH("refresh", "刷新令牌");

    private final String value;
    private final String desc;

    public static TokenTypeEnum fromValue(String value) {
        for (TokenTypeEnum type : TokenTypeEnum.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
