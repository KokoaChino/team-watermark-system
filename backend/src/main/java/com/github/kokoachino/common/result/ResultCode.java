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
    UNSUPPORTED_LOGIN_TYPE(1009, "不支持的登录方式"),
    LOCK_ACQUIRE_FAILED(1010, "获取锁失败，请稍后重试"),
    INVALID_PASSWORD(1011, "原密码错误"),
    EMAIL_ALREADY_BOUND(1012, "该邮箱已被其他用户绑定"),
    SAME_EMAIL(1013, "新邮箱不能与当前邮箱相同"),
    SAME_USER(1014, "新用户名不能与当前用户名相同"),
    TEAM_NOT_FOUND(2001, "团队不存在"),
    INVITE_CODE_INVALID(2002, "邀请码无效"),
    INVITE_CODE_EXPIRED(2003, "邀请码已过期"),
    INVITE_CODE_USED_UP(2004, "邀请码使用次数已达上限"),
    ALREADY_IN_TEAM(2005, "您已经在团队中"),
    NOT_TEAM_LEADER(2006, "只有队长可以执行此操作"),
    CANNOT_KICK_SELF(2007, "不能踢出自己"),
    MEMBER_NOT_FOUND(2008, "成员不存在"),
    CANNOT_LEAVE_PERSONAL_TEAM(2009, "不能退出个人团队"),
    INVITE_CODE_NOT_FOUND(2010, "邀请码不存在");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
