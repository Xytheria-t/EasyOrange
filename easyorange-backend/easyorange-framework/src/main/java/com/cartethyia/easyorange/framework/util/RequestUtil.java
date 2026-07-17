package com.cartethyia.easyorange.framework.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public final class RequestUtil {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    private static final Set<String> LOCALHOST_IPS = Set.of(
            LOCALHOST_IPV4,
            "::1",
            "0:0:0:0:0:0:0:1",
            "::ffff:127.0.0.1"
    );

    private RequestUtil() {
    }

    public static HttpServletRequest getRequest() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    public static String getClientIp() {
        var req = getRequest();
        return req != null ? getClientIp(req) : UNKNOWN;
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        var ip = Stream.of(HEADER_X_FORWARDED_FOR, HEADER_X_REAL_IP)
                .map(h -> extractFirstIpFromHeader(request, h))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(request::getRemoteAddr);
        return LOCALHOST_IPS.contains(ip) ? LOCALHOST_IPV4 : ip;
    }

    public static String getRequestPath() {
        var req = getRequest();
        return req != null ? req.getRequestURI() : "";
    }

    public static String getFullRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        var url = request.getRequestURL().toString();
        var query = request.getQueryString();
        return query != null && !query.isEmpty() ? url + "?" + query : url;
    }

    private static String extractFirstIpFromHeader(HttpServletRequest request, String header) {
        var value = request.getHeader(header);
        if (value == null || value.isBlank() || UNKNOWN.equalsIgnoreCase(value)) {
            return null;
        }
        var comma = value.indexOf(',');
        return comma >= 0 ? value.substring(0, comma).trim() : value.trim();
    }
}