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
 * <p>
 * 值类型是 {@link ChatAnswer}（而非 {@code Object}）：framework 的 {@code imageProcessCache} 同为 Caffeine 缓存，
 * 两者按泛型区分后注入点即可唯一解析，消费方（{@code AiChatService}）不必再用 {@code @Qualifier} 字符串限定。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiStaleCacheConfig {

    private final AiProperties aiProperties;

    @Bean("aiStaleCache")
    public Cache<String, ChatAnswer> aiStaleCache() {
        var props = aiProperties.cache();
        log.info("AI stale cache initialized: maxSize={}, expire={}h", props.staleMaxSize(), props.staleExpireHours());
        return Caffeine.newBuilder()
                .maximumSize(props.staleMaxSize())
                .expireAfterWrite(props.staleExpireHours(), TimeUnit.HOURS)
                .build();
    }
}
