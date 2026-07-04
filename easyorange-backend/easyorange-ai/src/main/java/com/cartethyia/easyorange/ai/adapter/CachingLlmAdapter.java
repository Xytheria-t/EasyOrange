package com.cartethyia.easyorange.ai.adapter;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.metrics.AiMetricsService;
import com.cartethyia.easyorange.ai.port.LlmPort;
import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Primary
@Component
public class CachingLlmAdapter implements LlmPort {

    private final DeepSeekLlmAdapter delegate;
    private final AiProperties aiProperties;
    private final Map<AiCallScope, MultiLevelCache> aiCaches;
    private final Cache<String, Object> staleCache;
    private final AiMetricsService aiMetricsService;

    public CachingLlmAdapter(
            DeepSeekLlmAdapter delegate,
            AiProperties aiProperties,
            @Qualifier("aiCaches") Map<AiCallScope, MultiLevelCache> aiCaches,
            @Qualifier("aiStaleCache") Cache<String, Object> staleCache,
            AiMetricsService aiMetricsService) {
        this.delegate = delegate;
        this.aiProperties = aiProperties;
        this.aiCaches = aiCaches;
        this.staleCache = staleCache;
        this.aiMetricsService = aiMetricsService;
    }

    @Override
    public String generateText(String systemPrompt, String userMessage) {
        return cached(AiCallScope.REVIEW, systemPrompt, userMessage,
                () -> delegate.generateText(systemPrompt, userMessage));
    }

    @Override
    public String generateTextWithJson(String systemPrompt, String userMessage) {
        return cached(AiCallScope.REVIEW, systemPrompt, userMessage,
                () -> delegate.generateTextWithJson(systemPrompt, userMessage));
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        return delegate.generateEmbedding(text);
    }

    private String cached(AiCallScope scope, String system, String user,
                          java.util.function.Supplier<String> loader) {
        if (!aiProperties.getCache().isEnabled()) {
            return loader.get();
        }

        String fp = fingerprint(system, user);
        String key = scope.cacheKeyPrefix() + fp;

        var cache = aiCaches.get(scope);
        if (cache == null) {
            log.warn("No cache for scope {}, fallback to delegate", scope);
            return loader.get();
        }

        var cacheMiss = new AtomicBoolean(false);
        String result = cache.get(key, String.class, () -> {
            cacheMiss.set(true);
            return loader.get();
        });

        if (cacheMiss.get()) {
            aiMetricsService.recordCacheMiss(scope.name());
        } else {
            aiMetricsService.recordCacheHit(scope.name());
        }

        if (result != null) {
            staleCache.put(key, result);
        }

        return result;
    }

    private static String fingerprint(String system, String user) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            var combined = (system != null ? system : "") + "|" + (user != null ? user : "");
            byte[] digest = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder(32);
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
