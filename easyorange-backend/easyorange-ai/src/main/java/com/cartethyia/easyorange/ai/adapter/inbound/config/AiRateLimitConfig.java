package com.cartethyia.easyorange.ai.adapter.inbound.config;

import com.cartethyia.easyorange.ai.adapter.inbound.web.AiRateLimitInterceptor;
import com.cartethyia.easyorange.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * AI 限流装配 — 注册 {@link AiRateLimitInterceptor} 拦截 {@code /api/ai/**}，超限返回 429。
 * <p>
 * 缓存不在本类：多级响应缓存随 Spring AI 迁移一并删除，仅存的 stale 缓存（LLM 供应商故障兜底，
 * 消费方 {@code AiChatService}）在 {@link com.cartethyia.easyorange.ai.config.AiStaleCacheConfig}。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiRateLimitConfig implements WebMvcConfigurer {

    private final AiProperties aiProperties;
    private final AiRateLimitInterceptor aiRateLimitInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        if (aiProperties.rateLimit().enabled()) {
            registry.addInterceptor(aiRateLimitInterceptor)
                    .addPathPatterns("/api/ai/**")
                    .order(0);
            log.info("AI rate limit interceptor registered for /api/ai/**");
        }
    }
}
