package com.example.demo.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;

public final class WebUtil {
    public static String findIp(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String ip = headers.getFirst("x-forwarded-for");
        if (!StringUtils.hasLength(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.getFirst("x-real-ip");
        } else if (ip.contains(",")) {
            // get first ip for X-Forwarded-For
            ip = ip.substring(0, ip.indexOf(","));
        }
        if (!StringUtils.hasLength(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        }

        return ip;
    }
}
