package com.github.kokoachino.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 登录类型枚举
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Getter
@AllArgsConstructor
public enum LoginTypeEnum {

    PASSWORD("password", "密码登录"),
    CAPTCHA("captcha", "验证码登录");

    private final String value;
    private final String desc;

    public static LoginTypeEnum fromValue(String value) {
        for (LoginTypeEnum type : LoginTypeEnum.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
