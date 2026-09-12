package com.health.assistant.common.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtils {

    private static final String UNKNOWN = "unknown";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";

    private ClientIpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader(X_FORWARDED_FOR);
        if (isValid(ip)) {
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        ip = request.getHeader(X_REAL_IP);
        if (isValid(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private static boolean isValid(String ip) {
        return ip != null && !ip.isBlank() && !UNKNOWN.equalsIgnoreCase(ip);
    }
}
