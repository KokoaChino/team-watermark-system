package com.github.kokoachino.common.util;

import java.util.concurrent.ThreadLocalRandom;


/**
 * 随机字符串工具类
 *
 * @author kokoachino
 * @date 2026-01-31
 */
public class RandomStringUtils {

    private static final char[] CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    public static String generate(int n) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        char[] buf = new char[n];
        for (int i = 0; i < n; i++) {
            buf[i] = CHARS[random.nextInt(CHARS.length)];
        }
        return new String(buf);
    }

    public static void main(String[] args) {
        System.out.println(generate(32));
    }
}
