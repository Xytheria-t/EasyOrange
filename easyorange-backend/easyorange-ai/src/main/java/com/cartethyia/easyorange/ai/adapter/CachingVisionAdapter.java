package com.cartethyia.easyorange.ai.adapter;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.metrics.AiMetricsService;
import com.cartethyia.easyorange.ai.port.VisionPort;
import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Slf4j
@Primary
@Component
public class CachingVisionAdapter implements VisionPort {

    private final QwenVlVisionAdapter delegate;
    private final AiProperties aiProperties;
    private final Map<AiCallScope, MultiLevelCache> aiCaches;
    private final Cache<String, Object> staleCache;
    private final AiMetricsService aiMetricsService;
    private final Retry aiRetry;

    public CachingVisionAdapter(
            QwenVlVisionAdapter delegate,
            AiProperties aiProperties,
            @Qualifier("aiCaches") Map<AiCallScope, MultiLevelCache> aiCaches,
            @Qualifier("aiStaleCache") Cache<String, Object> staleCache,
            AiMetricsService aiMetricsService,
            @Qualifier("aiVision") Retry aiRetry) {
        this.delegate = delegate;
        this.aiProperties = aiProperties;
        this.aiCaches = aiCaches;
        this.staleCache = staleCache;
        this.aiMetricsService = aiMetricsService;
        this.aiRetry = aiRetry;
    }

    @Override
    public String analyzeImage(String imageUrl, String prompt) {
        return analyzeImages(List.of(imageUrl), prompt);
    }

    @Override
    public String analyzeImages(List<String> imageUrls, String prompt) {
        // Resilience4j Retry 包装：网络故障时指数退避重试（最多 3 次）
        Supplier<String> retriedLoader = Retry.decorateSupplier(aiRetry,
                () -> delegate.analyzeImages(imageUrls, prompt));

        if (!aiProperties.getCache().isEnabled()) {
            return retriedLoader.get();
        }

        var scope = AiCallScope.AUTO_LISTING;
        String fp = fingerprintImages(imageUrls, prompt);
        String key = scope.cacheKeyPrefix() + fp;

        var cache = aiCaches.get(scope);
        if (cache == null) {
            return retriedLoader.get();
        }

        var cacheMiss = new AtomicBoolean(false);
        String result = cache.get(key, String.class, () -> {
            cacheMiss.set(true);
            return retriedLoader.get();
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

    private static String fingerprintImages(List<String> urls, String prompt) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            var sorted = new ArrayList<>(urls != null ? urls : List.of());
            Collections.sort(sorted);
            var combined = String.join("|", sorted) + "||" + (prompt != null ? prompt : "");
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
