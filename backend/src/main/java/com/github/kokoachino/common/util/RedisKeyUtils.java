package com.github.kokoachino.common.util;


/**
 * @author kokoachino
 * @date 2026-02-02
 */
public class RedisKeyUtils {

    private static final String CAPTCHA_PREFIX = "captcha：";
    private static final String EMAIL_CODE_PREFIX = "email_code：";

    public static String getCaptchaKey(String key) {
        return CAPTCHA_PREFIX + key;
    }

    public static String getEmailCodeKey(String type, String email) {
        return EMAIL_CODE_PREFIX + type + "：" + email;
    }
}
