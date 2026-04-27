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
    private static final String[] PROXY_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    /**
     * 可配置的信任代理 IP 列表（IP 白名单）
     * 只有来自这些 IP 的请求才信任其代理头
     */
    private static volatile String[] trustedProxies = new String[]{
            "127.0.0.1", "localhost", "0:0:0:0:0:0:0:1"
    };

    /**
     * 初始化标志，防止重复设置信任代理
     */
    private static volatile boolean initialized = false;

    /**
     * 锁对象，保证线程安全
     */
    private static final Object LOCK = new Object();

    private RequestUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 设置信任的代理 IP 列表
     * <p>
     * 仅当请求来自这些 IP 时，才信任 X-Forwarded-For、X-Real-IP 等代理头。
     * 这可以防止攻击者通过伪造代理头进行 IP 欺骗。
     * </p>
     * <p>
     * <strong>注意：</strong> 此方法只能调用一次，重复调用将抛出异常。
     * 建议在应用启动配置时设置，运行时不应修改。
     * </p>
     *
     * @param proxies 信任的代理 IP 数组
     * @throws IllegalStateException 如果已经设置过信任代理
     */
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

    /**
     * 重置信任代理设置（仅用于测试）
     */
    static void resetForTesting() {
        synchronized (LOCK) {
            trustedProxies = new String[]{"127.0.0.1", "localhost", "0:0:0:0:0:0:0:1"};
            initialized = false;
        }
    }

    /**
     * 检查当前请求的远程地址是否来自可信代理
     */
    private static boolean isFromTrustedProxy(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        for (String trusted : trustedProxies) {
            if (trusted.equalsIgnoreCase(remoteAddr)) {
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

        String ip = isFromTrustedProxy(request)
                ? extractIpFromProxyHeaders(request)
                : null;

        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        return LOCALHOST_IPV6.equals(ip) ? LOCALHOST_IPV4 : ip;
    }

    /**
     * 从代理头中提取 IP 地址
     * <p>
     * 按优先级检查各种代理头，返回第一个有效的 IP
     * </p>
     *
     * @param request HttpServletRequest
     * @return 从代理头获取的 IP，无有效值时返回 null
     */
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

    /**
     * 检查 IP 是否有效（非 null、非空、非 unknown）
     *
     * @param ip 待检查的 IP 字符串
     * @return true 表示有效，false 表示无效
     */
    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip);
    }

    /**
     * 获取完整的请求 URL
     *
     * @param request HttpServletRequest
     * @return 完整 URL，如 {@code http://localhost:8080/api/user/info}
     */
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

    /**
     * 获取当前请求路径（从 RequestContextHolder 中获取）
     *
     * @return 请求 URI，如 /api/user/info
     */
    public static String getRequestPath() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (attributes != null) ? attributes.getRequest().getRequestURI() : "";
    }

    /**
     * 获取当前 HttpServletRequest 对象（从 RequestContextHolder 中获取）
     *
     * @return HttpServletRequest，无请求上下文时返回 null
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (attributes != null) ? attributes.getRequest() : null;
    }
}
