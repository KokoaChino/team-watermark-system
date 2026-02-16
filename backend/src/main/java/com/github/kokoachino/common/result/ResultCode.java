package com.github.kokoachino.common.result;

import lombok.Getter;


/**
 * 返回码枚举类
 *
 * 编码规范：
 * - 0-999：通用错误码
 * - 1000-1999：用户认证相关
 * - 2000-2999：团队相关
 * - 3000-3999：水印模板相关
 * - 4000-4999：批量任务相关
 * - 5000-5999：点数与支付相关
 * - 6000-6999：系统相关（预留）
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Getter
public enum ResultCode {

    /* ==================== 通用错误码 (0-999) ==================== */
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    VALIDATE_FAILED(400, "参数检验失败"),
    UNAUTHORIZED(401, "暂未登录或token已经过期"),
    FORBIDDEN(403, "没有相关权限"),
    NOT_FOUND(404, "资源不存在"),
    REQUEST_TOO_FREQUENT(429, "请求过于频繁，请稍后再试"),
    LOCK_ACQUIRE_FAILED(430, "获取锁失败，请稍后重试"),

    /* ==================== 用户认证相关 (1000-1999) ==================== */
    // 1000-1099：用户基础
    USER_NOT_FOUND(1000, "用户不存在"),
    USER_EXIST(1001, "用户已存在"),
    USERNAME_ALREADY_EXIST(1002, "用户名已被使用"),
    USER_DISABLED(1003, "用户已被禁用"),

    // 1100-1199：密码相关
    PASSWORD_ERROR(1100, "密码错误"),
    INVALID_PASSWORD(1101, "原密码错误"),
    PASSWORD_TOO_WEAK(1102, "密码强度不足"),

    // 1200-1299：邮箱相关
    EMAIL_EXIST(1200, "邮箱已被注册"),
    EMAIL_NOT_FOUND(1201, "邮箱不存在"),
    EMAIL_CODE_ERROR(1202, "邮箱验证码错误"),
    EMAIL_CODE_EXPIRED(1203, "邮箱验证码已过期"),
    SEND_MAIL_FAILED(1204, "邮件发送失败"),
    EMAIL_ALREADY_BOUND(1205, "该邮箱已被其他用户绑定"),
    SAME_EMAIL(1206, "新邮箱不能与当前邮箱相同"),

    // 1300-1399：验证码相关
    CAPTCHA_ERROR(1300, "验证码错误"),
    CAPTCHA_EXPIRED(1301, "验证码已过期"),

    // 1400-1499：登录相关
    UNSUPPORTED_LOGIN_TYPE(1400, "不支持的登录方式"),
    TOKEN_INVALID(1401, "token无效"),
    TOKEN_EXPIRED(1402, "token已过期"),

    // 1500-1599：用户信息修改
    SAME_USER(1500, "新用户名不能与当前用户名相同"),

    /* ==================== 团队相关 (2000-2999) ==================== */
    // 2000-2099：团队基础
    TEAM_NOT_FOUND(2000, "团队不存在"),
    TEAM_NAME_EXIST(2001, "团队名称已存在"),
    TEAM_FULL(2002, "团队人数已达上限"),

    // 2100-2199：邀请码相关
    INVITE_CODE_INVALID(2100, "邀请码无效"),
    INVITE_CODE_EXPIRED(2101, "邀请码已过期"),
    INVITE_CODE_USED_UP(2102, "邀请码使用次数已达上限"),
    INVITE_CODE_NOT_FOUND(2103, "邀请码不存在"),

    // 2200-2299：成员管理
    ALREADY_IN_TEAM(2200, "您已经在团队中"),
    NOT_TEAM_LEADER(2201, "只有队长可以执行此操作"),
    CANNOT_KICK_SELF(2202, "不能踢出自己"),
    MEMBER_NOT_FOUND(2203, "成员不存在"),
    CANNOT_LEAVE_PERSONAL_TEAM(2204, "不能退出个人团队"),
    NOT_TEAM_MEMBER(2205, "不是团队成员"),
    CANNOT_TRANSFER_TO_SELF(2206, "不能将队长身份转让给自己"),

    /* ==================== 水印模板相关 (3000-3999) ==================== */
    // 3000-3099：模板基础
    TEMPLATE_NOT_FOUND(3000, "水印模板不存在"),
    TEMPLATE_NAME_EXIST(3001, "模板名称已存在"),
    TEMPLATE_VERSION_CONFLICT(3002, "模板已被他人修改，请刷新后重试"),
    TEMPLATE_DELETED(3003, "该模板已被删除"),
    NOT_TEMPLATE_CREATOR(3004, "只有模板创建者或队长可以删除模板"),

    // 3100-3199：草稿相关
    DRAFT_NOT_FOUND(3100, "草稿不存在"),
    NO_WORKING_DRAFT(3101, "当前没有正在编辑的草稿"),

    // 3200-3299：字体相关
    FONT_NOT_FOUND(3200, "字体不存在"),
    FONT_NAME_EXIST(3201, "字体名称已存在"),
    FONT_FILE_INVALID(3202, "字体文件格式不正确，仅支持.ttf和.otf格式"),
    FONT_UPLOAD_FAILED(3203, "字体上传失败"),

    /* ==================== 批量任务相关 (4000-4999) ==================== */
    // 4000-4099：任务基础
    BATCH_TASK_NOT_FOUND(4000, "批量任务不存在"),
    BATCH_TASK_SUBMIT_FAILED(4001, "任务提交失败"),
    TASK_ALREADY_COMPLETED(4002, "任务已完成，不能重复操作"),

    // 4100-4199：Excel解析
    EXCEL_PARSE_ERROR(4100, "Excel解析失败"),
    EXCEL_STRUCTURE_INVALID(4101, "Excel结构不正确"),
    IMAGE_NOT_FOUND_IN_EXCEL(4102, "Excel中未找到对应的图片配置"),

    // 4200-4299：任务执行（预留）

    /* ==================== 点数与支付相关 (5000-5999) ==================== */
    // 5000-5099：点数相关
    POINTS_NOT_ENOUGH(5000, "团队点数不足"),
    POINT_TRANSACTION_FAILED(5001, "点数交易失败"),

    // 5100-5199：支付相关
    PAYMENT_ORDER_NOT_FOUND(5100, "支付订单不存在"),
    PAYMENT_CREATE_FAILED(5101, "创建支付订单失败"),
    PAYMENT_VERIFY_FAILED(5102, "支付验证失败"),
    PAYMENT_ALREADY_PAID(5103, "订单已支付"),
    PAYMENT_AMOUNT_INVALID(5104, "支付金额无效"),

    // 5200-5299：退款相关（预留）

    /* ==================== 系统相关 (6000-6999) 预留 ==================== */
    SYSTEM_ERROR(6000, "系统错误"),
    DATABASE_ERROR(6001, "数据库错误"),
    CACHE_ERROR(6002, "缓存错误"),
    FILE_UPLOAD_ERROR(6003, "文件上传失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
