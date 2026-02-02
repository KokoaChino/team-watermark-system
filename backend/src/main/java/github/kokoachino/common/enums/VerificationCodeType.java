package github.kokoachino.common.enums;

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
public enum VerificationCodeType {

    REGISTER("register", "注册验证码"),
    FORGOT_PASSWORD("forgot_password", "找回密码验证码"),
    LOGIN("login", "登录验证码");

    private final String value;
    private final String desc;

    public static VerificationCodeType fromValue(String value) {
        for (VerificationCodeType type : VerificationCodeType.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
