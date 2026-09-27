package com.cartethyia.easyorange.ai.adapter.outbound.cache;

import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.model.VectorUtils;
import com.cartethyia.easyorange.ai.domain.port.SemanticCachePort;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * 语义缓存 — 相似问题复用历史回答（成本优化的核心落地）：查询向量化后与缓存条目算余弦，
 * 超阈值即命中。写入走 Redis Hash（{@code eo:ai:semantic:<scope>:<用户桶>}），条目超上限淘汰最旧；
 * Redis / embedding 任一不可用都 fail-open（不命中不阻塞）。
 * <p>
 * <b>按用户分桶</b>（正确性要求而非调优项）：条目存的是注入了该用户长期画像与会话历史的回答，
 * 共享桶会把一个人的偏好返给另一个人；匿名会话收敛到单一桶（不注入画像，共享安全），代价是
 * 跨用户的近似问题不再互相命中 —— 这是修正确性，不是牺牲命中率换调优。
 * <p>
 * <b>向量按 base64 float32 存</b>，不用 JSON 数字数组：1024 维按 JSON 数组约 10KB，而每次查询都要
 * 把整个 Hash 拉回逐条算余弦（O(n) 扫描），base64 压到约 4KB 且不走浮点文本解析；容量默认 200 同理
 * —— 真到了需要更大容量的量级，应换向量索引（ES kNN）而不是继续加大这个 Hash。
 * <p>
 * <b>单条脏数据不影响整次查询</b>：格式不符或向量解码失败的条目直接跳过，旧格式条目随 TTL /
 * 淘汰自然过期，不做迁移。一次查询只算一次向量的调用约定见 {@link SemanticCachePort}。
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class SemanticCacheService implements SemanticCachePort {

    private static final String KEY_PREFIX = "eo:ai:semantic:";

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final AiModelSupport aiModelSupport;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    /** 查询向量化 — 按 {@link AiCallScope#CHAT} 记账：命中一次就是一次真实供应商调用，不落日志不计预算就永远是账外项。 */
    @Override
    public List<Float> embedQuery(String query) {
        if (!aiProperties.semanticCache().enabled() || query == null || query.isBlank()) {
            return List.of();
        }
        var embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            return List.of();
        }
        try {
            return aiModelSupport.embed(embeddingModel, AiCallScope.CHAT, query);
        } catch (Exception e) {
            log.warn("Semantic cache query embedding failed, skip cache for this call", e);
            return List.of();
        }
    }

    /** 语义命中则返回缓存响应，否则 empty（命中只在同一用户桶内比较，分桶口径见 {@link SemanticCachePort#lookUp}）。 */
    @Override
    public <T> Optional<T> lookUp(
            AiCallScope scope, @Nullable String userId, String query, List<Float> queryEmbedding, Class<T> type) {
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            return Optional.empty();
        }
        var redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return Optional.empty();
        }
        try {
            double threshold = aiProperties.semanticCache().similarityThreshold();
            Map<Object, Object> entries = redis.opsForHash().entries(key(scope, userId));
            String bestResponse = null;
            double bestSimilarity = threshold;
            for (Object raw : entries.values()) {
                CachedEntry entry;
                try {
                    entry = objectMapper.readValue((String) raw, CachedEntry.class);
                } catch (Exception e) {
                    continue;
                }
                Double similarity = similarityOf(queryEmbedding, entry);
                if (similarity != null && similarity > bestSimilarity) {
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

    /** 写入缓存 (queryEmbedding, response)；淘汰上限按用户桶各算各的 —— 高频用户的桶满了不该把其他用户的条目挤掉。 */
    @Override
    public void store(
            AiCallScope scope, @Nullable String userId, String query, List<Float> queryEmbedding, Object response) {
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            return;
        }
        var redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            String field = md5(query);
            String value = objectMapper.writeValueAsString(new CachedEntry(
                    encodeVector(queryEmbedding),
                    objectMapper.writeValueAsString(response),
                    System.currentTimeMillis()));
            String key = key(scope, userId);
            Long size = redis.opsForHash().size(key);
            if (size != null && size >= aiProperties.semanticCache().maxEntries()) {
                evictOldest(redis, key);
            }
            redis.opsForHash().put(key, field, value);
            redis.expire(key, Duration.ofHours(aiProperties.semanticCache().ttlHours()));
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

    /** 余弦相似度；条目损坏（旧格式 / 向量解码失败）返回 null，由调用方跳过该条。 */
    private static Double similarityOf(List<Float> queryEmbedding, CachedEntry entry) {
        try {
            return VectorUtils.cosine(queryEmbedding, decodeVector(entry.vector()));
        } catch (Exception e) {
            return null;
        }
    }

    /** base64 float32 编码（包可见静态便于单测直接构造缓存条目）。 */
    static String encodeVector(List<Float> vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.size() * Float.BYTES);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    static List<Float> decodeVector(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        var vector = new ArrayList<Float>(bytes.length / Float.BYTES);
        while (buffer.remaining() >= Float.BYTES) {
            vector.add(buffer.getFloat());
        }
        return vector;
    }

    /** Redis key = 前缀 + scope + <b>用户桶</b>（正确性要求，见类注释）；加桶前的旧 key 随 TTL 自然过期，不做迁移。 */
    private static String key(AiCallScope scope, @Nullable String userId) {
        return KEY_PREFIX + scope.name().toLowerCase() + ':' + SemanticCachePort.cacheUserKey(userId);
    }

    private static String md5(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }

    /** Redis Hash 条目：查询向量（base64 float32）+ 序列化响应 + 时间戳。字段名 vector 与旧版 embedding 不同，旧条目解析失败即被跳过、自然过期，不做迁移。 */
    private record CachedEntry(String vector, String response, long timestamp) {}
}
