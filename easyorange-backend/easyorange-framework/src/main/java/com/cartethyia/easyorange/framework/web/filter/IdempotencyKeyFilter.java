package com.cartethyia.easyorange.framework.web.filter;

import com.cartethyia.easyorange.framework.config.properties.IdempotencyProperties;
import com.cartethyia.easyorange.framework.web.idempotency.CachedResponse;
import com.cartethyia.easyorange.framework.web.idempotency.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Idempotency-Key 幂等过滤器（替代原 {@code IdempotencyAspect} AOP 方案）：客户端在写请求头携带
 * {@code Idempotency-Key}，同一 key 的业务操作只执行一次，首次请求执行并缓存成功响应，重复请求回放缓存响应。
 * <p>
 * 缓存的是<b>序列化后的 HTTP 响应</b>（status + contentType + body）而非控制器返回的类型化对象——字节级精确
 * 回放客户端真正看到的内容；落在 {@code AnonymousAuthenticationFilter} 之前，才能包住认证后到 DispatcherServlet
 * 之间的整段链、抓到最终响应字节。Redis 不可用时 {@code IdempotencyService} fail-open 放行（降级为无幂等保护）。
 * <p>
 * 由 {@code SecurityConfig} 局部装配（不加 {@code @Component}）：执行位置由 Security 链的
 * {@code addFilterBefore} 决定；容器链再自动注册一次只会让顺序变成两套事实。
 */
@Slf4j
@NullMarked
public class IdempotencyKeyFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final IdempotencyService idempotencyService;
    private final IdempotencyProperties properties;

    public IdempotencyKeyFilter(IdempotencyService idempotencyService, IdempotencyProperties properties) {
        this.idempotencyService = idempotencyService;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.enabled()) {
            return true;
        }
        if (!properties.methods().contains(request.getMethod())) {
            return true;
        }
        String key = request.getHeader(properties.headerName());
        if (key == null || key.isBlank()) {
            return true;
        }
        return properties.pathPatterns().stream()
                .noneMatch(pattern -> PATH_MATCHER.match(pattern, request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getHeader(properties.headerName());
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            CachedResponse cached = idempotencyService.execute(
                    key,
                    properties.defaultTtlSeconds(),
                    () -> executeAndCapture(request, response, wrappedResponse, filterChain));
            replay(response, cached);
        } catch (NonCacheableResponseException e) {
            // 错误响应已在 executeAndCapture 内提交，不再缓存；直接结束
            log.debug("action=idempotency_not_cached, key={}, uri={}", key, request.getRequestURI());
        } catch (ServletException | IOException e) {
            throw e;
        } catch (Exception e) {
            // 幂等执行过程中的其它异常（如 Redis 层异常）→ 包装为 ServletException 透传
            throw new ServletException("idempotency execute failed, key=" + key, e);
        }
    }

    /** 执行后续链路并抓取响应；非 2xx 不缓存，提交后抛异常中断缓存。 */
    private CachedResponse executeAndCapture(
            HttpServletRequest request,
            HttpServletResponse realResponse,
            ContentCachingResponseWrapper wrappedResponse,
            FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, wrappedResponse);
        CachedResponse cached = new CachedResponse(
                wrappedResponse.getStatus(), wrappedResponse.getContentType(), wrappedResponse.getContentAsByteArray());
        if (cached.status() >= 400) {
            replay(realResponse, cached);
            throw NonCacheableResponseException.INSTANCE;
        }
        return cached;
    }

    /** 将缓存/抓取的响应回放到真实响应。 */
    private static void replay(HttpServletResponse response, CachedResponse cached) throws IOException {
        response.setStatus(cached.status());
        if (cached.contentType() != null) {
            response.setContentType(cached.contentType());
        }
        response.setContentLength(cached.body().length);
        response.getOutputStream().write(cached.body());
        response.flushBuffer();
    }

    /** 标记非 2xx 响应已提交、不得缓存。 */
    private static final class NonCacheableResponseException extends RuntimeException {
        private static final NonCacheableResponseException INSTANCE = new NonCacheableResponseException();

        private NonCacheableResponseException() {
            super(null, null, false, false); // 禁用 stack trace 与 suppression
        }
    }
}
