package com.github.kokoachino.common.util;

import jakarta.servlet.http.HttpServletRequest;


/**
 * 请求 IP 工具类
 *
 * @author Kokoa_Chino
 * @date 2026-03-09
 */
public class RequestIpUtils {

    private RequestIpUtils() {}

    /**
     * 获取客户端 IP 地址
     *
     * @param request 当前请求
     * @return 客户端 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
