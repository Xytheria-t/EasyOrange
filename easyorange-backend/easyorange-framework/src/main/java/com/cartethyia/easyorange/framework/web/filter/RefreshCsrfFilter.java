package com.cartethyia.easyorange.framework.web.filter;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.config.properties.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Refresh 端点 CSRF 纵深防御过滤器。
 * <p>
 * 对 csrf-protected 路径（默认 refresh/logout）的 POST 强制要求自定义头 X-Client-Type：
 * 浏览器跨站请求无法伪造自定义头（会触发预检并被同源策略拦截），从 Cookie 读取 refresh 的流程靠此防 CSRF。
 * 自定义头缺失返回 403。路径由 {@link SecurityProperties#getCsrfProtectedPaths()} 配置驱动。
 */
@Component
@RequiredArgsConstructor
@NullMarked
public class RefreshCsrfFilter extends OncePerRequestFilter {

    private static final String REQUIRED_HEADER = "X-Client-Type";

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return securityProperties.getCsrfProtectedPaths().stream()
                .noneMatch(path -> path.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getHeader(REQUIRED_HEADER) == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getOutputStream(),
                    Result.error(ResultCode.FORBIDDEN, "缺少自定义请求头，疑似跨站请求"));
            return;
        }
        chain.doFilter(request, response);
    }
}