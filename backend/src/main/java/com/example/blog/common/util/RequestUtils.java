package com.example.blog.common.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class RequestUtils {

    private RequestUtils() {
    }

    public static HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    public static String getClientIp() {
        return getClientIp(currentRequest());
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String[] headerKeys = {"X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP"};
        for (String key : headerKeys) {
            String header = request.getHeader(key);
            if (header != null && !header.isBlank()) {
                return header.split(",")[0].trim();
            }
        }
        return Optional.ofNullable(request.getRemoteAddr()).orElse("unknown");
    }

    public static String getUserAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "unknown";
        }
        return Optional.ofNullable(request.getHeader("User-Agent")).orElse("unknown");
    }
}
