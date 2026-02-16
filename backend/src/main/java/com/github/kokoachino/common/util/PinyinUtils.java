package com.github.kokoachino.common.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;


/**
 * 拼音工具类
 *
 * @author Kokoa_Chino
 * @date 2026-02-16
 */
public class PinyinUtils {

    private static final HanyuPinyinOutputFormat FORMAT;

    static {
        FORMAT = new HanyuPinyinOutputFormat();
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        FORMAT.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    /**
     * 将字符串转换为拼音
     *
     * @param str 中文字符串
     * @return 拼音字符串
     */
    public static String toPinyin(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        StringBuilder pinyin = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.toString(c).matches("[\\u4E00-\\u9FA5]+")) {
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        pinyin.append(pinyinArray[0]);
                    }
                } catch (BadHanyuPinyinOutputFormatCombination ignored) {
                    pinyin.append(c);
                }
            } else {
                pinyin.append(c);
            }
        }
        return pinyin.toString().toLowerCase();
    }
}
