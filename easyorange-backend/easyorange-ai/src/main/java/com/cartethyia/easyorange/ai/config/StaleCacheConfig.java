package com.cartethyia.easyorange.ai.config;

import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 供应商故障兜底的本地旧回答缓存。
 * 值类型是 {@link ChatAnswer} 而非 {@code Object}：framework 的 {@code imageProcessCache} 同为 Caffeine 缓存，
 * 两者按泛型区分后注入点唯一解析，消费方（{@code ChatAppService}）不必写 {@code @Qualifier} 字符串限定。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StaleCacheConfig {

    private final AiProperties aiProperties;

    @Bean("aiStaleCache")
    public Cache<String, ChatAnswer> aiStaleCache() {
        var props = aiProperties.staleCache();
        log.info("AI stale cache initialized: maxSize={}, expire={}h", props.maxSize(), props.expireHours());
        return Caffeine.newBuilder()
                .maximumSize(props.maxSize())
                .expireAfterWrite(props.expireHours(), TimeUnit.HOURS)
                .build();
    }
}
