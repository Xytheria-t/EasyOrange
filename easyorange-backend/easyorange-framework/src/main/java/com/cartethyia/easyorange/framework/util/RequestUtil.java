package com.cartethyia.easyorange.framework.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class RequestUtil {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String[] PROXY_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private static volatile String[] trustedProxies = new String[]{
            "127.0.0.1", "localhost", "0:0:0:0:0:0:0:1"
    };

    private static volatile boolean initialized = false;

    private static final Object LOCK = new Object();

    private RequestUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static void setTrustedProxies(String... proxies) {
        if (initialized && proxies != null && proxies.length > 0) {
            throw new IllegalStateException("Trust proxies already initialized. Cannot be modified at runtime.");
        }
        synchronized (LOCK) {
            if (!initialized && proxies != null && proxies.length > 0) {
                trustedProxies = proxies.clone();
                initialized = true;
            }
        }
    }

    static void resetForTesting() {
        synchronized (LOCK) {
            trustedProxies = new String[]{"127.0.0.1", "localhost", "0:0:0:0:0:0:0:1"};
            initialized = false;
        }
    }

    private static boolean isFromTrustedProxy(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        for (String trusted : trustedProxies) {
            if (trusted.equalsIgnoreCase(remoteAddr)) {
                return true;
            }
        }
        return false;
    }

    public static String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return UNKNOWN;
        }
        return getClientIp(attributes.getRequest());
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ip = isFromTrustedProxy(request)
                ? extractIpFromProxyHeaders(request)
                : null;

        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        return LOCALHOST_IPV6.equals(ip) ? LOCALHOST_IPV4 : ip;
    }

    private static String extractIpFromProxyHeaders(HttpServletRequest request) {
        for (String header : PROXY_HEADERS) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                int commaIndex = ip.indexOf(',');
                return (commaIndex > 0) ? ip.substring(0, commaIndex).trim() : ip.trim();
            }
        }

        return null;
    }

    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip);
    }

    public static String getFullRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        StringBuilder url = new StringBuilder(request.getRequestURL());
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            url.append("?").append(queryString);
        }
        return url.toString();
    }

    public static String getRequestPath() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (attributes != null) ? attributes.getRequest().getRequestURI() : "";
    }

    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (attributes != null) ? attributes.getRequest() : null;
    }
}
