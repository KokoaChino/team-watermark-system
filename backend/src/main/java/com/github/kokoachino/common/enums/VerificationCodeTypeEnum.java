package com.github.kokoachino.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 验证码类型枚举
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Getter
@AllArgsConstructor
public enum VerificationCodeTypeEnum {

    REGISTER("register", "注册验证码"),
    FORGOT_PASSWORD("forgot_password", "找回密码验证码"),
    LOGIN("login", "登录验证码"),
    UPDATE_EMAIL("update_email", "修改邮箱验证码");

    private final String value;
    private final String desc;

    public static VerificationCodeTypeEnum fromValue(String value) {
        for (VerificationCodeTypeEnum type : VerificationCodeTypeEnum.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
