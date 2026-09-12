package com.health.assistant.common.web;

import com.health.assistant.common.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;

public record ClientInfo(String ip, String userAgent) {

    public static ClientInfo from(HttpServletRequest request) {
        return new ClientInfo(ClientIpUtils.getClientIp(request), request.getHeader("User-Agent"));
    }
}
