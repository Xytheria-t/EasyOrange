package com.cartethyia.easyorange.framework.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

/**
 * 请求工具类 —— 从 HttpServletRequest 中提取信息。
 * <p>全静态方法，无状态，无需配置。</p>
 */
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

    /**
     * 从当前线程绑定的请求中获取客户端 IP。无请求上下文时返回 "unknown"。
     */
    public static String getClientIp() {
        HttpServletRequest req = getCurrentRequest();
        return req != null ? getClientIp(req) : UNKNOWN;
    }

    /**
     * 从请求中获取客户端真实 IP，按以下顺序：
     * <ol>
     *   <li>{@code X-Forwarded-For}（多代理时取最左 IP）</li>
     *   <li>{@code X-Real-IP}</li>
     *   <li>{@code getRemoteAddr()}</li>
     * </ol>
     * IPv6 环回地址统一归一化至 {@code 127.0.0.1}。
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String ip = tryHeader(request, HEADER_X_FORWARDED_FOR);
        if (ip == null) {
            ip = tryHeader(request, HEADER_X_REAL_IP);
        }
        if (ip == null) {
            ip = request.getRemoteAddr();
        }
        return LOCALHOST_IPS.contains(ip) ? LOCALHOST_IPV4 : ip;
    }

    /**
     * 获取当前请求路径。
     */
    public static String getRequestPath() {
        HttpServletRequest req = getCurrentRequest();
        return req != null ? req.getRequestURI() : "";
    }

    /**
     * 构建完整请求 URL（含查询参数）。
     */
    public static String getFullRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String url = request.getRequestURL().toString();
        String query = request.getQueryString();
        return query != null && !query.isEmpty() ? url + "?" + query : url;
    }

    /**
     * 获取当前 HttpServletRequest。无请求上下文时返回 null。
     */
    public static HttpServletRequest getRequest() {
        return getCurrentRequest();
    }

    /**
     * 获取当前线程绑定的原生请求对象
     */
    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    /**
     * 从指定请求头中获取内容。
     * 若值为空、空白或为 unknown（忽略大小写），则返回 null；
     * 存在多个值（逗号分隔）时，只取第一个并去除首尾空格。
     *
     * @param request HTTP 请求对象
     * @param header  请求头名称
     * @return 处理后的头信息，无效内容返回 null
     */

    private static String tryHeader(HttpServletRequest request, String header) {
        String value = request.getHeader(header);
        if (value == null || value.isEmpty() || UNKNOWN.equalsIgnoreCase(value)) {
            return null;
        }
        int comma = value.indexOf(',');
        return comma >= 0 ? value.substring(0, comma).trim() : value.trim();
    }
}