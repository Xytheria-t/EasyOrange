package com.cartethyia.easyorange.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求工具类
 * <p>
 * 提供获取客户端 IP、请求 URL 等常用方法。
 * </p>
 *
 * @author cartethyia
 */
public final class RequestUtil {

    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_IPV4 = "127.0.0.1";

    /**
     * 可配置的信任代理列表（IP 白名单）
     * 只有来自这些 IP 的请求才信任其代理头
     */
    private static volatile String[] trustedProxies = new String[]{
            "127.0.0.1", "localhost", "0:0:0:0:0:0:0:1"
    };

    private RequestUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 设置信任的代理 IP 列表
     * <p>
     * 仅当请求来自这些 IP 时，才信任 X-Forwarded-For、X-Real-IP 等代理头。
     * 这可以防止攻击者通过伪造代理头进行 IP 欺骗。
     * </p>
     *
     * @param proxies 信任的代理 IP 数组
     */
    public static void setTrustedProxies(String... proxies) {
        if (proxies != null && proxies.length > 0) {
            trustedProxies = proxies.clone();
        }
    }

    /**
     * 检查当前请求的远程地址是否来自可信代理
     */
    private static boolean isFromTrustedProxy(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        for (String trusted : trustedProxies) {
            if (trusted.equals(remoteAddr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取客户端 IP 地址（从当前请求上下文自动获取）
     */
    public static String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return UNKNOWN;
        }
        return getClientIp(attributes.getRequest());
    }

    /**
     * 获取客户端 IP 地址
     * <p>
     * 优先从 X-Forwarded-For、X-Real-IP 等代理头中获取，适用于反向代理场景。
     * 仅当请求来自可信代理时，才信任代理头，防止 IP 欺骗攻击。
     * </p>
     *
     * @param request HttpServletRequest
     * @return 客户端 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        // 只有来自可信代理的请求才信任代理头
        boolean isTrustedProxy = isFromTrustedProxy(request);

        String ip = null;
        if (isTrustedProxy) {
            // 信任代理头，按优先级获取
            ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
        }

        // 如果没有从代理头获取到 IP，使用原始 remoteAddr
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For 可能包含多个 IP（逗号分隔），取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // IPv6 本地回环转 IPv4
        if (LOCALHOST_IPV6.equals(ip)) {
            ip = LOCALHOST_IPV4;
        }

        return ip;
    }

    /**
     * 获取完整的请求 URL
     *
     * @param request HttpServletRequest
     * @return 完整 URL，如 http://localhost:8080/api/user/info
     */
    public static String getFullRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String url = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            url += "?" + queryString;
        }
        return url;
    }

    /**
     * 获取当前请求路径（从 RequestContextHolder 中获取）
     *
     * @return 请求 URI，如 /api/user/info
     */
    public static String getRequestPath() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "";
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getRequestURI();
    }

    /**
     * 获取当前 HttpServletRequest 对象（从 RequestContextHolder 中获取）
     *
     * @return HttpServletRequest，无请求上下文时返回 null
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
