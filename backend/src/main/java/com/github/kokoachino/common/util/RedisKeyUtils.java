package com.github.kokoachino.common.util;


/**
 * Redis Key 管理工具类
 *
 * @author kokoachino
 * @date 2026-02-02
 */
public class RedisKeyUtils {

    private static final String CAPTCHA_PREFIX = "captcha：";
    private static final String EMAIL_CODE_PREFIX = "email_code：";
    private static final String USER_PREFIX = "user：";

    /**
     * 获取图形验证码 Key
     */
    public static String getCaptchaKey(String key) {
        return CAPTCHA_PREFIX + key;
    }

    /**
     * 获取邮箱验证码 Key
     */
    public static String getEmailCodeKey(String type, String email) {
        return EMAIL_CODE_PREFIX + type + "：" + email;
    }

    /**
     * 获取用户缓存 Key
     */
    public static String getUserKey(Integer userId) {
        return USER_PREFIX + userId;
    }
}
