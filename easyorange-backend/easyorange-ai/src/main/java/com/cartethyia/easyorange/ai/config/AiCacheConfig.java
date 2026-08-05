package com.cartethyia.easyorange.ai.config;

import com.cartethyia.easyorange.ai.interceptor.AiRateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * AI Web 配置 — 注册 {@link AiRateLimitInterceptor} 拦截 {@code /api/ai/**}。
 * <p>
 * 多级响应缓存（L1 Caffeine + L2 Redis + 跨节点失效）已随 {@code CachingLlmAdapter}/
 * {@code CachingVisionAdapter} 一并移除（2026-08 迁移至 Spring AI），
 * 仅保留 stale 缓存（AiStaleCacheConfig）供限流降级返回。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiCacheConfig implements WebMvcConfigurer {

    private final AiProperties aiProperties;
    private final AiRateLimitInterceptor aiRateLimitInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        if (aiProperties.getRateLimit().isEnabled()) {
            registry.addInterceptor(aiRateLimitInterceptor)
                    .addPathPatterns("/api/ai/**")
                    .order(0);
            log.info("AI rate limit interceptor registered for /api/ai/**");
        }
    }
}
