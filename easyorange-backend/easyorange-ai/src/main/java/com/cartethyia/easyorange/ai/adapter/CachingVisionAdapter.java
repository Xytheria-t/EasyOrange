package com.cartethyia.easyorange.ai.adapter;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.port.VisionPort;
import com.cartethyia.easyorange.framework.cache.MultiLevelCache;
import com.github.benmanes.caffeine.cache.Cache;
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

@Slf4j
@Primary
@Component
public class CachingVisionAdapter implements VisionPort {

    private final QwenVlVisionAdapter delegate;
    private final AiProperties aiProperties;
    private final Map<AiCallScope, MultiLevelCache> aiCaches;
    private final Cache<String, Object> staleCache;

    public CachingVisionAdapter(
            QwenVlVisionAdapter delegate,
            AiProperties aiProperties,
            @Qualifier("aiCaches") Map<AiCallScope, MultiLevelCache> aiCaches,
            @Qualifier("aiStaleCache") Cache<String, Object> staleCache) {
        this.delegate = delegate;
        this.aiProperties = aiProperties;
        this.aiCaches = aiCaches;
        this.staleCache = staleCache;
    }

    @Override
    public String analyzeImage(String imageUrl, String prompt) {
        return analyzeImages(List.of(imageUrl), prompt);
    }

    @Override
    public String analyzeImages(List<String> imageUrls, String prompt) {
        if (!aiProperties.getCache().isEnabled()) {
            return delegate.analyzeImages(imageUrls, prompt);
        }

        var scope = AiCallScope.AUTO_LISTING;
        String fp = fingerprintImages(imageUrls, prompt);
        String key = scope.cacheKeyPrefix() + fp;

        var cache = aiCaches.get(scope);
        if (cache == null) {
            return delegate.analyzeImages(imageUrls, prompt);
        }

        String result = cache.get(key, String.class,
                () -> delegate.analyzeImages(imageUrls, prompt));

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
