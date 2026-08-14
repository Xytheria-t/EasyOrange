package com.cartethyia.easyorange.ai.adapter.outbound.cache;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.knowledge.VectorUtils;
import com.cartethyia.easyorange.ai.service.AiModelSupport;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * 语义缓存 — 相似问题复用历史回答（成本优化的核心落地）。
 * <p>
 * 命中判定：查询先向量化，与缓存条目的 embedding 做余弦相似度，超过阈值即命中，
 * 相同/近似问题不再调 LLM。写入走 Redis Hash（{@code eo:ai:semantic:<scope>}），
 * 条目数超上限淘汰最旧；Redis / embedding 任一不可用都 fail-open（不命中不阻塞）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SemanticCacheService {

    private static final String KEY_PREFIX = "eo:ai:semantic:";

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final AiModelSupport aiModelSupport;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    /**
     * 语义命中则返回缓存响应，否则 empty。
     */
    public <T> Optional<T> get(AiCallScope scope, String query, Class<T> type) {
        if (!aiProperties.getSemanticCache().isEnabled() || query == null || query.isBlank()) {
            return Optional.empty();
        }
        var redis = redisProvider.getIfAvailable();
        var embeddingModel = embeddingModelProvider.getIfAvailable();
        if (redis == null || embeddingModel == null) {
            return Optional.empty();
        }
        try {
            List<Float> queryEmbedding = aiModelSupport.embed(embeddingModel, query);
            double threshold = aiProperties.getSemanticCache().getSimilarityThreshold();
            Map<Object, Object> entries = redis.opsForHash().entries(key(scope));
            String bestResponse = null;
            double bestSimilarity = threshold;
            for (Object raw : entries.values()) {
                CachedEntry entry = objectMapper.readValue((String) raw, CachedEntry.class);
                double similarity = VectorUtils.cosine(queryEmbedding, entry.embedding());
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestResponse = entry.response();
                }
            }
            return bestResponse == null ? Optional.empty() : Optional.of(objectMapper.readValue(bestResponse, type));
        } catch (Exception e) {
            log.warn("Semantic cache read failed, miss", e);
            return Optional.empty();
        }
    }

    /**
     * 写入缓存：embed 查询 → 存 (queryEmbedding, response)；超出 maxEntries 淘汰最旧条目。
     */
    public void put(AiCallScope scope, String query, Object response) {
        if (!aiProperties.getSemanticCache().isEnabled() || query == null || query.isBlank()) {
            return;
        }
        var redis = redisProvider.getIfAvailable();
        var embeddingModel = embeddingModelProvider.getIfAvailable();
        if (redis == null || embeddingModel == null) {
            return;
        }
        try {
            List<Float> queryEmbedding = aiModelSupport.embed(embeddingModel, query);
            String field = md5(query);
            String value = objectMapper.writeValueAsString(new CachedEntry(
                    queryEmbedding, objectMapper.writeValueAsString(response), System.currentTimeMillis()));
            String key = key(scope);
            Long size = redis.opsForHash().size(key);
            if (size != null && size >= aiProperties.getSemanticCache().getMaxEntries()) {
                evictOldest(redis, key);
            }
            redis.opsForHash().put(key, field, value);
            redis.expire(key, Duration.ofHours(aiProperties.getSemanticCache().getTtlHours()));
        } catch (Exception e) {
            log.warn("Semantic cache write failed, skip", e);
        }
    }

    private void evictOldest(StringRedisTemplate redis, String key) {
        Map<Object, Object> entries = redis.opsForHash().entries(key);
        Object oldestField = null;
        long oldestTs = Long.MAX_VALUE;
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                CachedEntry cached = objectMapper.readValue((String) entry.getValue(), CachedEntry.class);
                if (cached.timestamp() < oldestTs) {
                    oldestTs = cached.timestamp();
                    oldestField = entry.getKey();
                }
            } catch (Exception ignored) {
                // 脏条目随最旧一起淘汰
            }
        }
        if (oldestField != null) {
            redis.opsForHash().delete(key, oldestField);
        }
    }

    private static String key(AiCallScope scope) {
        return KEY_PREFIX + scope.name().toLowerCase();
    }

    private static String md5(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }

    /** Redis Hash 中的缓存条目：查询向量 + 序列化后的响应 + 写入时间戳。 */
    private record CachedEntry(List<Float> embedding, String response, long timestamp) {}
}
