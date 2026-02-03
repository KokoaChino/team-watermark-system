package com.github.kokoachino.common.result;

import lombok.Getter;


/**
 * 返回码枚举类
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    VALIDATE_FAILED(404, "参数检验失败"),
    UNAUTHORIZED(401, "暂未登录或token已经过期"),
    FORBIDDEN(403, "没有相关权限"),
    USER_NOT_FOUND(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    CAPTCHA_ERROR(1003, "验证码错误"),
    USER_EXIST(1004, "用户已存在"),
    EMAIL_EXIST(1005, "邮箱已存在"),
    EMAIL_CODE_ERROR(1006, "邮箱验证码错误"),
    REQUEST_TOO_FREQUENT(1007, "请求过于频繁，请稍后再试"),
    SEND_MAIL_FAILED(1008, "邮件发送失败"),
    UNSUPPORTED_LOGIN_TYPE(1009, "不支持的登录方式");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
