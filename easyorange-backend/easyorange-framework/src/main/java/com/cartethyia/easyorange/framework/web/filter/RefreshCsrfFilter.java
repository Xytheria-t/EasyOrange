package com.cartethyia.easyorange.framework.web.filter;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.framework.config.properties.SecurityProperties;
import com.cartethyia.easyorange.framework.web.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refresh 端点 CSRF 纵深防御过滤器。
 * <p>
 * 对 csrf-protected 路径（默认 refresh/logout）的 POST 强制要求自定义头 X-Client-Type：
 * 浏览器跨站请求无法伪造自定义头（会触发预检并被同源策略拦截），从 Cookie 读取 refresh 的流程靠此防 CSRF。
 * 自定义头缺失返回 403。路径由 {@link SecurityProperties#getCsrfProtectedPaths()} 配置驱动。
 *
 * <p>
 * 由 {@code SecurityConfig} 局部装配（不加 {@code @Component}）：执行位置由 Security 链的
 * {@code addFilterBefore} 决定；容器链再自动注册一次只会让顺序变成两套事实。
 * </p>
 */
@RequiredArgsConstructor
@NullMarked
public class RefreshCsrfFilter extends OncePerRequestFilter {

    private static final String REQUIRED_HEADER = "X-Client-Type";

    private final SecurityProperties securityProperties;
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return securityProperties.csrfProtectedPaths().stream().noneMatch(path -> path.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getHeader(REQUIRED_HEADER) == null) {
            errorResponseWriter.write(
                    response, HttpServletResponse.SC_FORBIDDEN, ResultCode.FORBIDDEN, "缺少自定义请求头，疑似跨站请求");
            return;
        }
        chain.doFilter(request, response);
    }
}
