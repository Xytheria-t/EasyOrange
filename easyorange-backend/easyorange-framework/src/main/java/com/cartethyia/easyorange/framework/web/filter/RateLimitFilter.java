package com.cartethyia.easyorange.framework.web.filter;

import com.cartethyia.easyorange.common.annotation.SkipRateLimit;
import com.cartethyia.easyorange.common.annotation.SkipRepeatSubmit;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.config.properties.RateLimitFilterProperties;
import com.cartethyia.easyorange.framework.config.properties.RateLimitFilterProperties.RepeatSubmitConfig;
import com.cartethyia.easyorange.framework.config.properties.RateLimitFilterProperties.Rule;
import com.cartethyia.easyorange.framework.util.DistributedRateLimiter;
import com.cartethyia.easyorange.framework.util.LocalRateLimiter;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import org.springframework.data.redis.core.RedisTemplate;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
public class RateLimitFilter extends OncePerRequestFilter {

    private static final HexFormat HEX_FORMAT = HexFormat.of();
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    private final RateLimitFilterProperties properties;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final LocalRateLimiter localRateLimiter;
    private final DistributedRateLimiter distributedRateLimiter;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<List<HandlerMapping>> handlerMappingsProvider;

    public RateLimitFilter(RateLimitFilterProperties properties,
                           RedisTemplate<Object, Object> redisTemplate,
                           LocalRateLimiter localRateLimiter,
                           DistributedRateLimiter distributedRateLimiter,
                           ObjectMapper objectMapper,
                           ObjectProvider<List<HandlerMapping>> handlerMappingsProvider) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.localRateLimiter = localRateLimiter;
        this.distributedRateLimiter = distributedRateLimiter;
        this.objectMapper = objectMapper;
        this.handlerMappingsProvider = handlerMappingsProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod().toUpperCase(Locale.ROOT);

        // 写请求需要缓存 body 以便 filter 和 controller 都能读取
        CachedBodyHttpServletRequestWrapper wrappedRequest = null;
        if (WRITE_METHODS.contains(method)) {
            wrappedRequest = new CachedBodyHttpServletRequestWrapper(request);
        }

        try {
            HttpServletRequest effectiveRequest = wrappedRequest != null ? wrappedRequest : request;

            // 限流 — 只在命中规则且方法没有 @SkipRateLimit 时检查
            Rule matchedRule = findMatchingRule(method, effectiveRequest.getRequestURI());
            if (matchedRule != null && !hasSkipAnnotation(effectiveRequest, SkipRateLimit.class)) {
                checkRateLimit(effectiveRequest, method, matchedRule);
            }

            // 防重 — 写方法且没有 @SkipRepeatSubmit 时检查
            if (WRITE_METHODS.contains(method) && !hasSkipAnnotation(effectiveRequest, SkipRepeatSubmit.class)) {
                checkRepeatSubmit(effectiveRequest, method, wrappedRequest.getCachedBody());
            }

            filterChain.doFilter(wrappedRequest != null ? wrappedRequest : request, response);
        } catch (BusinessException ex) {
            writeErrorResponse(response, ex);
        }
    }

    private Rule findMatchingRule(String method, String uri) {
        for (Rule rule : properties.getRules()) {
            if (matchesMethod(rule, method) && PATH_MATCHER.match(rule.getPathPattern(), uri)) {
                return rule;
            }
        }
        return null;
    }

    private boolean matchesMethod(Rule rule, String method) {
        if (rule.getMethods().isEmpty()) {
            return true;
        }
        return rule.getMethods().stream()
                .anyMatch(m -> m.equalsIgnoreCase(method));
    }

    // ==================== 限流 ====================

    private void checkRateLimit(HttpServletRequest request, String method, Rule rule) {
        if ("local".equalsIgnoreCase(rule.getStrategy())) {
            checkLocalRateLimit(request, method, rule);
        } else {
            checkRedisRateLimit(request, method, rule);
        }
    }

    private void checkLocalRateLimit(HttpServletRequest request, String method, Rule rule) {
        long windowMs = TimeUnit.SECONDS.toMillis(rule.getWindowSeconds());
        if (windowMs <= 0 || rule.getMaxRequests() <= 0) {
            return;
        }

        String key = RequestUtil.getClientIp(request) + ":" + method + ":" + request.getRequestURI();
        if (!localRateLimiter.tryAcquire(key, rule.getMaxRequests(), windowMs)) {
            log.warn("action=local_rate_limit, key={}, limit={}", key, rule.getMaxRequests());
            throw BusinessException.of(rule.getMessage());
        }
    }

    private void checkRedisRateLimit(HttpServletRequest request, String method, Rule rule) {
        if (rule.getWindowSeconds() <= 0 || rule.getMaxRequests() <= 0) {
            return;
        }

        String identifier = RequestUtil.getClientIp(request);
        String key = "eo:rate:" + identifier + ":" + method + ":" + request.getRequestURI();
        try {
            // Redisson RRateLimiter 令牌桶 — 原子化取桶/补桶/扣桶，解决 increment+expire 的原子性缺口
            boolean allowed = distributedRateLimiter.tryAcquire(
                    key, rule.getMaxRequests(), rule.getWindowSeconds());
            if (!allowed) {
                log.warn("action=redis_rate_limit, key={}, limit={}", key, rule.getMaxRequests());
                throw BusinessException.of(rule.getMessage());
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
        RepeatSubmitConfig config = properties.getRepeatSubmit();
        if (!config.isEnabled()) {
            return;
        }
        if (!config.getMethods().isEmpty()
                && config.getMethods().stream().noneMatch(m -> m.equalsIgnoreCase(method))) {
            return;
        }

        long intervalMs = config.getIntervalMs();
        if (intervalMs <= 0) {
            return;
        }

        // 用 IP 作为用户标识（Filter 在认证之前执行，无法获取 userId）
        String userIdentifier = RequestUtil.getClientIp(request);

        // key 包含请求体 hash，不同参数的请求不会被误判为重复
        String bodyHash = md5(cachedBody);
        String key = "eo:repeat:" + userIdentifier + ":" + request.getRequestURI() + ":" + bodyHash;

        try {
            if (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", intervalMs, TimeUnit.MILLISECONDS))) {
                throw BusinessException.of(config.getMessage());
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("action=repeat_submit_check_error, key={}", key, ex);
            // Redis 不可用时放行（fail-open）
        }
    }

    // ==================== Skip 注解检查 ====================

    /**
     * 检查目标 Controller 方法/类是否有 Skip 注解。
     * 无法解析 handler 时返回 false（放行默认规则），
     * Spring {@link AbstractHandlerMapping} 内部缓存了 handler + 方法级检查结果，
     * 同一次请求多次调用无额外开销。
     */
    private boolean hasSkipAnnotation(HttpServletRequest request, Class<? extends Annotation> annotationClass) {
        List<HandlerMapping> handlerMappings = handlerMappingsProvider.getIfAvailable(List::of);
        for (HandlerMapping mapping : handlerMappings) {
            try {
                HandlerExecutionChain chain = mapping.getHandler(request);
                if (chain != null && chain.getHandler() instanceof HandlerMethod hm) {
                    return hm.getMethodAnnotation(annotationClass) != null
                            || hm.getBeanType().isAnnotationPresent(annotationClass);
                }
            } catch (Exception e) {
                log.debug("Failed to resolve handler via {}: {}", mapping, e.toString());
            }
        }
        return false;
    }

    // ==================== 工具方法 ====================

    private void writeErrorResponse(HttpServletResponse response, BusinessException ex) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<Void> result = Result.error(ex.getCode(), ex.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private String md5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input);
            return HEX_FORMAT.formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }
}
