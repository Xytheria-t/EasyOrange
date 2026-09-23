package com.cartethyia.easyorange.framework.web.filter;

import com.cartethyia.easyorange.common.annotation.SkipRateLimit;
import com.cartethyia.easyorange.common.annotation.SkipRepeatSubmit;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.config.properties.RateLimitFilterProperties;
import com.cartethyia.easyorange.framework.config.properties.RateLimitFilterProperties.RepeatSubmitConfig;
import com.cartethyia.easyorange.framework.config.properties.RateLimitFilterProperties.Rule;
import com.cartethyia.easyorange.framework.config.properties.RateLimitFilterProperties.Strategy;
import com.cartethyia.easyorange.framework.util.DistributedRateLimiter;
import com.cartethyia.easyorange.framework.util.LocalRateLimiter;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import com.cartethyia.easyorange.framework.web.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.DigestUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 限流 + 防重提交统一过滤器
 * <p>
 * 替代原 {@code RateLimiterAspect} 和 {@code RepeatSubmitAspect}，
 * 通过配置驱动实现约定式自动防护。
 * </p>
 * <ul>
 *   <li>限流：GET 走本地内存，写操作走 Redis 分布式限流</li>
 *   <li>防重：写操作自动防重（Redis SETNX），key 包含请求体 hash</li>
 *   <li>降级：Redis 不可用时放行请求（fail-open）</li>
 * </ul>
 */
@Slf4j
@Component
@Order(0)
@NullMarked
public class RateLimitFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    private final RateLimitFilterProperties properties;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final LocalRateLimiter localRateLimiter;
    private final DistributedRateLimiter distributedRateLimiter;
    private final ErrorResponseWriter errorResponseWriter;
    private final ObjectProvider<List<HandlerMapping>> handlerMappingsProvider;

    public RateLimitFilter(
            RateLimitFilterProperties properties,
            RedisTemplate<Object, Object> redisTemplate,
            LocalRateLimiter localRateLimiter,
            DistributedRateLimiter distributedRateLimiter,
            ErrorResponseWriter errorResponseWriter,
            ObjectProvider<List<HandlerMapping>> handlerMappingsProvider) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.localRateLimiter = localRateLimiter;
        this.distributedRateLimiter = distributedRateLimiter;
        this.errorResponseWriter = errorResponseWriter;
        this.handlerMappingsProvider = handlerMappingsProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod().toUpperCase(Locale.ROOT);

        // 写请求需要缓存 body 以便 filter 和 controller 都能读取；multipart 例外，见 isMultipart
        @Nullable CachedBodyHttpServletRequestWrapper wrappedRequest = null;
        if (WRITE_METHODS.contains(method) && !isMultipart(request)) {
            wrappedRequest = new CachedBodyHttpServletRequestWrapper(request);
        }

        try {
            HttpServletRequest effectiveRequest = wrappedRequest != null ? wrappedRequest : request;

            // 限流 — 只在命中规则且方法没有 @SkipRateLimit 时检查
            Rule matchedRule = findMatchingRule(method, effectiveRequest.getRequestURI());

            // 命中规则或写请求时才需要解析 handler；仅解析一次供限流/防重复用，避免每请求重复解析
            @Nullable
            HandlerMethod handlerMethod =
                    (matchedRule != null || WRITE_METHODS.contains(method)) ? resolveHandler(effectiveRequest) : null;

            if (matchedRule != null && !hasSkipAnnotation(handlerMethod, SkipRateLimit.class)) {
                checkRateLimit(effectiveRequest, method, matchedRule);
            }

            // 防重 — 有缓存 body 的写请求且没有 @SkipRepeatSubmit 时检查
            // （wrappedRequest 非空 ⟺ 写方法且非 multipart，防重 key 依赖 body hash）
            if (wrappedRequest != null && !hasSkipAnnotation(handlerMethod, SkipRepeatSubmit.class)) {
                checkRepeatSubmit(effectiveRequest, method, wrappedRequest.getCachedBody());
            }

            filterChain.doFilter(wrappedRequest != null ? wrappedRequest : request, response);
        } catch (BusinessException ex) {
            writeErrorResponse(response, ex);
        }
    }

    /**
     * multipart 请求不能预读 body：{@link CachedBodyHttpServletRequestWrapper} 构造时就把原始输入流读空，
     * 容器随后解析 parts 只能拿到已耗尽的流（MultipartException → 400，文件上传必挂）。
     * 代价是这类请求没有 body 可算防重 key，跳过防重检查 —— 用「不拦截」换「能上传」：
     * 重复上传的代价只是多一条文件记录，而误判（键退化成 URI+IP）会挡住正常上传。
     */
    private static boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
    }

    private @Nullable Rule findMatchingRule(String method, String uri) {
        for (Rule rule : properties.rules()) {
            if (matchesMethod(rule, method) && PATH_MATCHER.match(rule.pathPattern(), uri)) {
                return rule;
            }
        }
        return null;
    }

    private boolean matchesMethod(Rule rule, String method) {
        if (rule.methods().isEmpty()) {
            return true;
        }
        return rule.methods().stream().anyMatch(m -> m.equalsIgnoreCase(method));
    }

    // ==================== 限流 ====================

    private void checkRateLimit(HttpServletRequest request, String method, Rule rule) {
        if (rule.strategy() == Strategy.LOCAL) {
            checkLocalRateLimit(request, method, rule);
        } else {
            checkRedisRateLimit(request, method, rule);
        }
    }

    private void checkLocalRateLimit(HttpServletRequest request, String method, Rule rule) {
        long windowMs = TimeUnit.SECONDS.toMillis(rule.windowSeconds());
        if (windowMs <= 0 || rule.maxRequests() <= 0) {
            return;
        }

        String key = RequestUtil.getClientIp(request) + ":" + method + ":" + request.getRequestURI();
        if (!localRateLimiter.tryAcquire(key, rule.maxRequests(), windowMs)) {
            log.warn("action=local_rate_limit, key={}, limit={}", key, rule.maxRequests());
            throw BusinessException.of(rule.message());
        }
    }

    private void checkRedisRateLimit(HttpServletRequest request, String method, Rule rule) {
        if (rule.windowSeconds() <= 0 || rule.maxRequests() <= 0) {
            return;
        }

        String identifier = RequestUtil.getClientIp(request);
        String key = "eo:rate:" + identifier + ":" + method + ":" + request.getRequestURI();
        try {
            // Redisson RRateLimiter 令牌桶 — 原子化取桶/补桶/扣桶，解决 increment+expire 的原子性缺口
            boolean allowed = distributedRateLimiter.tryAcquire(key, rule.maxRequests(), rule.windowSeconds());
            if (!allowed) {
                log.warn("action=redis_rate_limit, key={}, limit={}", key, rule.maxRequests());
                throw BusinessException.of(rule.message());
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("action=redis_rate_limit_error, key={}", key, ex);
            // Redis 不可用时放行（fail-open）
        }
    }

    // ==================== 防重提交 ====================

    private void checkRepeatSubmit(HttpServletRequest request, String method, byte[] cachedBody) {
        RepeatSubmitConfig config = properties.repeatSubmit();
        if (!config.enabled()) {
            return;
        }
        if (isRepeatSubmitExcluded(request.getRequestURI())) {
            return;
        }
        if (!config.methods().isEmpty() && config.methods().stream().noneMatch(m -> m.equalsIgnoreCase(method))) {
            return;
        }

        long intervalMs = config.intervalMs();
        if (intervalMs <= 0) {
            return;
        }

        // 用 IP 作为用户标识（Filter 在认证之前执行，无法获取 userId）
        String userIdentifier = RequestUtil.getClientIp(request);

        // key 含方法 + 查询串 + 请求体 hash：同 URI 的不同方法（如收藏 POST / 取消收藏 DELETE）是不同操作；
        // 查询串必须入 key —— 发验证码这类参数走 @RequestParam（body 恒空），不带查询串会把
        // 「给不同手机号取码」误判成 3 秒内重复提交
        String bodyHash = md5(cachedBody);
        String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        String key =
                "eo:repeat:" + userIdentifier + ":" + method + ":" + request.getRequestURI() + query + ":" + bodyHash;

        try {
            if (Boolean.FALSE.equals(
                    redisTemplate.opsForValue().setIfAbsent(key, "1", intervalMs, TimeUnit.MILLISECONDS))) {
                throw BusinessException.of(config.message());
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("action=repeat_submit_check_error, key={}", key, ex);
            // Redis 不可用时放行（fail-open）
        }
    }

    /**
     * 豁免路径（{@code repeat-submit.exclude-path-patterns}）不做防重 —— 机器协议端点的
     * 重复请求体是协议内合法行为（JSON-RPC 超时重试复用同一请求体），拦掉会把 client
     * 卡死在错误循环里。
     */
    private boolean isRepeatSubmitExcluded(String uri) {
        for (String pattern : properties.repeatSubmit().excludePathPatterns()) {
            if (PATH_MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    // ==================== Skip 注解检查 ====================

    /**
     * 解析请求对应的目标 {@link HandlerMethod}，解析失败返回 {@code null}（放行默认规则）。
     * 每次请求至多解析一次，由调用方在命中规则或写请求时懒惰触发。
     */
    private @Nullable HandlerMethod resolveHandler(HttpServletRequest request) {
        List<HandlerMapping> handlerMappings = handlerMappingsProvider.getIfAvailable(List::of);
        for (HandlerMapping mapping : handlerMappings) {
            try {
                HandlerExecutionChain chain = mapping.getHandler(request);
                if (chain != null && chain.getHandler() instanceof HandlerMethod hm) {
                    return hm;
                }
            } catch (Exception e) {
                log.debug("Failed to resolve handler via {}: {}", mapping, e.toString());
            }
        }
        return null;
    }

    /**
     * 检查目标方法/类是否有 Skip 注解（方法级优先，其次类级）。
     */
    private boolean hasSkipAnnotation(
            @Nullable HandlerMethod handlerMethod, Class<? extends Annotation> annotationClass) {
        return handlerMethod != null
                && (handlerMethod.getMethodAnnotation(annotationClass) != null
                        || handlerMethod.getBeanType().isAnnotationPresent(annotationClass));
    }

    // ==================== 工具方法 ====================

    private void writeErrorResponse(HttpServletResponse response, BusinessException ex) throws IOException {
        errorResponseWriter.write(
                response, HttpStatus.TOO_MANY_REQUESTS.value(), ResultCode.TOO_MANY_REQUESTS, ex.getMessage());
    }

    private String md5(byte[] input) {
        return DigestUtils.md5DigestAsHex(input);
    }
}
