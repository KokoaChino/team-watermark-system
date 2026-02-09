package com.github.kokoachino.common.util;

import cn.hutool.core.util.RandomUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 邀请码工具类
 * 支持生成和从文本中提取邀请码
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
public class InviteCodeUtils {

    /**
     * 邀请码长度
     */
    private static final int CODE_LENGTH = 6;

    /**
     * 邀请码字符集（排除易混淆字符：0, O, 1, I, L）
     */
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    /**
     * 邀请码包装前缀
     */
    private static final String WRAP_PREFIX = "【";

    /**
     * 邀请码包装后缀
     */
    private static final String WRAP_SUFFIX = "】";

    /**
     * 从文本中提取邀请码的正则表达式
     * 匹配【】中的6位字母数字组合
     */
    private static final Pattern INVITE_CODE_PATTERN = Pattern.compile(
            WRAP_PREFIX + "([" + CODE_CHARS + "]{" + CODE_LENGTH + "})" + WRAP_SUFFIX
    );

    /**
     * 生成原始邀请码（6位字母数字）
     *
     * @return 6位邀请码
     */
    public static String generateRawCode() {
        return RandomUtil.randomString(CODE_CHARS, CODE_LENGTH);
    }

    /**
     * 生成带包装的邀请码文本
     * 格式：【XXXXXX】
     *
     * @param rawCode 原始邀请码
     * @return 包装后的邀请码
     */
    public static String wrapCode(String rawCode) {
        return WRAP_PREFIX + rawCode + WRAP_SUFFIX;
    }

    /**
     * 生成完整的邀请码分享文本
     *
     * @param teamName 团队名称
     * @param rawCode  原始邀请码
     * @return 分享文本
     */
    public static String generateShareText(String teamName, String rawCode) {
        return String.format("快来加入%s团队%s，一起协作处理图片水印吧！",
                teamName, wrapCode(rawCode));
    }

    /**
     * 从文本中提取邀请码
     *
     * @param text 包含邀请码的文本
     * @return 提取到的邀请码，如果没有找到则返回null
     */
    public static String extractCodeFromText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        Matcher matcher = INVITE_CODE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 验证邀请码格式是否正确
     *
     * @param code 邀请码
     * @return 是否有效
     */
    public static boolean isValidCodeFormat(String code) {
        if (code == null || code.length() != CODE_LENGTH) {
            return false;
        }
        for (char c : code.toCharArray()) {
            if (CODE_CHARS.indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }
}
