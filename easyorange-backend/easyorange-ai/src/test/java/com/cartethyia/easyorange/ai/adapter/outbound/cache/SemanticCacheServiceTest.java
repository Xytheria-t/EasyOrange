package com.cartethyia.easyorange.ai.adapter.outbound.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.cache.SemanticCacheMetrics;
import com.cartethyia.easyorange.ai.application.cache.SemanticCacheMetrics.Outcome;
import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import com.cartethyia.easyorange.ai.domain.model.ChatSource;
import com.cartethyia.easyorange.ai.testsupport.PropertyBindings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("SemanticCacheService (语义缓存) -> 测试")
class SemanticCacheServiceTest {

    // 必须在 CACHED_JSON 之前声明：静态字段按文本顺序初始化，helper 依赖它
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final List<Float> QUERY_VECTOR = List.of(1f, 0f, 0f);

    /** 被测调用的调用方身份：缓存 key 按它分桶（key 里不再只有 scope）。 */
    private static final String USER = "user-1";

    /** USER 对应的 Redis key —— 分桶后的实际 key，断言直接比对字面量。 */
    private static final String CACHE_KEY = "eo:ai:semantic:chat:" + USER;

    private static final String CACHED_JSON = cachedEntry(List.of(0.99f, 0.1f, 0f), "缓存回答", 1);

    @Mock
    private ObjectProvider<StringRedisTemplate> redisProvider;

    @Mock
    private ObjectProvider<EmbeddingModel> embeddingModelProvider;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private AiModelSupport aiModelSupport;

    private SemanticCacheService cache;

    /** 真实 registry 而非 mock：口径要断言的是「记到了哪个 outcome」，mock 只能验调用次数、验不了取值。 */
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        cache = cache(PropertyBindings.bind(AiProperties.class));
    }

    private SemanticCacheService cache(AiProperties aiProperties) {
        return new SemanticCacheService(
                redisProvider,
                embeddingModelProvider,
                aiModelSupport,
                aiProperties,
                new ObjectMapper(),
                new SemanticCacheMetrics(meterRegistry));
    }

    private double outcomeCount(Outcome outcome) {
        Counter counter = meterRegistry
                .find(SemanticCacheMetrics.CACHE_METRIC)
                .tag("outcome", outcome.getTag())
                .counter();
        return counter == null ? 0d : counter.count();
    }

    @Nested
    @DisplayName("embedQuery — 查询向量化（命中查找与写入共用的一次调用）")
    class EmbedQueryTests {

        @Test
        @DisplayName("正常 -> 返回查询向量")
        void embedQuery_ok() {
            when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
            when(aiModelSupport.embed(any(), any(), anyString())).thenReturn(QUERY_VECTOR);

            assertThat(cache.embedQuery("怎么退款？")).isEqualTo(QUERY_VECTOR);
        }

        @Test
        @DisplayName("缓存关闭 -> 空列表（调用方据此跳过缓存）")
        void embedQuery_disabled() {
            cache = cache(PropertyBindings.bind(AiProperties.class, "semantic-cache.enabled", "false"));

            assertThat(cache.embedQuery("问题")).isEmpty();
            verify(embeddingModelProvider, never()).getIfAvailable();
        }

        @Test
        @DisplayName("embedding 模型未配置 -> 空列表")
        void embedQuery_noModel() {
            when(embeddingModelProvider.getIfAvailable()).thenReturn(null);

            assertThat(cache.embedQuery("问题")).isEmpty();
        }

        @Test
        @DisplayName("空白查询 -> 空列表，不调模型")
        void embedQuery_blank() {
            assertThat(cache.embedQuery("  ")).isEmpty();
            verify(aiModelSupport, never()).embed(any(), any(), anyString());
        }

        @Test
        @DisplayName("向量化异常 -> 空列表（不抛出，缓存失败不阻塞问答）")
        void embedQuery_embeddingThrows() {
            when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
            when(aiModelSupport.embed(any(), any(), anyString())).thenThrow(new RuntimeException("dashscope timeout"));

            assertThat(cache.embedQuery("问题")).isEmpty();
        }
    }

    @Nested
    @DisplayName("lookUp — 余弦相似度命中")
    class LookUpTests {

        @Test
        @DisplayName("相似查询超过阈值 -> 命中返回缓存回答")
        void lookUp_hitOnSimilarQuery() {
            when(redisProvider.getIfAvailable()).thenReturn(redis);
            when(redis.opsForHash()).thenReturn(hashOps);
            when(hashOps.entries(CACHE_KEY)).thenReturn(Map.of("f1", CACHED_JSON));

            Optional<ChatAnswer> result = cache.lookUp(AiCallScope.CHAT, USER, "怎么退款？", QUERY_VECTOR, ChatAnswer.class);

            assertThat(result).isPresent();
            assertThat(result.get().answer()).isEqualTo("缓存回答");
        }

        @Test
        @DisplayName("不相似查询 -> 未命中")
        void lookUp_missOnDissimilar() {
            when(redisProvider.getIfAvailable()).thenReturn(redis);
            when(redis.opsForHash()).thenReturn(hashOps);
            String dissimilar = cachedEntry(List.of(0f, 1f, 0f), "缓存回答", 1);
            when(hashOps.entries(CACHE_KEY)).thenReturn(Map.of("f1", dissimilar));

            assertThat(cache.lookUp(AiCallScope.CHAT, USER, "怎么退款？", QUERY_VECTOR, ChatAnswer.class))
                    .isEmpty();
        }

        @Test
        @DisplayName("脏条目（旧格式/缺字段）被跳过，不影响同一批里的正常条目命中")
        void lookUp_skipsCorruptedEntries() {
            when(redisProvider.getIfAvailable()).thenReturn(redis);
            when(redis.opsForHash()).thenReturn(hashOps);
            // 旧格式（embedding 字段 + JSON 数组）与坏 base64 各一条，正常条目一条
            String legacy = """
                    {"embedding":[1.0,0.0,0.0],"response":"{}","timestamp":1}
                    """;
            String brokenVector = "{\"vector\":\"!!!not-base64!!!\",\"response\":\"{}\",\"timestamp\":2}";
            when(hashOps.entries(CACHE_KEY))
                    .thenReturn(Map.of("legacy", legacy, "broken", brokenVector, "good", CACHED_JSON));

            Optional<ChatAnswer> result = cache.lookUp(AiCallScope.CHAT, USER, "怎么退款？", QUERY_VECTOR, ChatAnswer.class);

            assertThat(result).as("一条脏数据不应把整次查询变成未命中").isPresent();
            assertThat(result.get().answer()).isEqualTo("缓存回答");
        }

        @Test
        @DisplayName("空查询向量（缓存不可用）-> 未命中，不访问 Redis")
        void lookUp_emptyEmbedding() {
            assertThat(cache.lookUp(AiCallScope.CHAT, USER, "问题", List.of(), ChatAnswer.class))
                    .isEmpty();
            verify(redisProvider, never()).getIfAvailable();
        }

        @Test
        @DisplayName("Redis 不可用 -> 未命中（fail-open）")
        void lookUp_noRedis() {
            when(redisProvider.getIfAvailable()).thenReturn(null);

            assertThat(cache.lookUp(AiCallScope.CHAT, USER, "问题", QUERY_VECTOR, ChatAnswer.class))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("用户隔离 — 缓存按身份分桶")
    class UserIsolationTests {

        @Test
        @DisplayName("同一问题不同用户 -> 读各自的桶，A 的答案不会命中 B")
        void lookUp_doesNotCrossUsers() {
            when(redisProvider.getIfAvailable()).thenReturn(redis);
            when(redis.opsForHash()).thenReturn(hashOps);
            // A 的桶里有高度相似的条目；B 的桶是空的
            when(hashOps.entries("eo:ai:semantic:chat:user-a")).thenReturn(Map.of("f1", CACHED_JSON));
            when(hashOps.entries("eo:ai:semantic:chat:user-b")).thenReturn(Map.of());

            assertThat(cache.lookUp(AiCallScope.CHAT, "user-a", "怎么退款？", QUERY_VECTOR, ChatAnswer.class))
                    .isPresent();
            assertThat(cache.lookUp(AiCallScope.CHAT, "user-b", "怎么退款？", QUERY_VECTOR, ChatAnswer.class))
                    .as("回答里注入了用户偏好，跨用户命中就是信息泄露")
                    .isEmpty();
        }

        @Test
        @DisplayName("分桶后不再读 scope 级旧 key（旧数据自然过期，不做迁移）")
        void doesNotReadLegacyScopeOnlyKey() {
            when(redisProvider.getIfAvailable()).thenReturn(redis);
            when(redis.opsForHash()).thenReturn(hashOps);
            when(hashOps.entries(CACHE_KEY)).thenReturn(Map.of());

            cache.lookUp(AiCallScope.CHAT, USER, "问题", QUERY_VECTOR, ChatAnswer.class);

            verify(hashOps, never()).entries("eo:ai:semantic:chat");
        }
    }

    @Nested
    @DisplayName("store — 写入与淘汰")
    class StoreTests {

        @Test
        @DisplayName("写入 -> 存 (embedding, response) 并刷新 TTL")
        void store_ok() {
            when(redisProvider.getIfAvailable()).thenReturn(redis);
            when(redis.opsForHash()).thenReturn(hashOps);
            when(hashOps.size(CACHE_KEY)).thenReturn(0L);

            cache.store(AiCallScope.CHAT, USER, "怎么退款？", QUERY_VECTOR, new ChatAnswer("回答", List.of(), "s", false));

            verify(hashOps).put(eq(CACHE_KEY), anyString(), anyString());
            verify(redis).expire(CACHE_KEY, Duration.ofHours(24));
        }

        @Test
        @DisplayName("条目超上限 -> 淘汰最旧一条再写入")
        void store_evictsOldest() {
            when(redisProvider.getIfAvailable()).thenReturn(redis);
            when(redis.opsForHash()).thenReturn(hashOps);
            when(hashOps.size(CACHE_KEY)).thenReturn(200L);
            String oldEntry = cachedEntry(QUERY_VECTOR, "旧", 100);
            String newEntry = cachedEntry(QUERY_VECTOR, "新", 200);
            when(hashOps.entries(CACHE_KEY)).thenReturn(Map.of("old", oldEntry, "new", newEntry));

            cache.store(AiCallScope.CHAT, USER, "问题", QUERY_VECTOR, new ChatAnswer("回答", List.of(), "s", false));

            verify(hashOps).delete(CACHE_KEY, "old");
        }

        @Test
        @DisplayName("空查询向量（缓存不可用）-> 跳过写入，不访问 Redis")
        void store_emptyEmbedding() {
            cache.store(AiCallScope.CHAT, USER, "问题", List.of(), new ChatAnswer("回答", List.of(), "s", false));

            verify(redisProvider, never()).getIfAvailable();
        }
    }

    @Test
    @DisplayName("store 写出的条目能被 lookUp 原样读回（写读对称：手写 fixture 可能与真实写入漂移，这条锁两端一致）")
    void store_thenLookUp_roundtrip() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForHash()).thenReturn(hashOps);
        when(hashOps.size(CACHE_KEY)).thenReturn(0L);
        var written = new java.util.concurrent.atomic.AtomicReference<String>();
        org.mockito.Mockito.doAnswer(inv -> {
                    written.set(inv.getArgument(2));
                    return null;
                })
                .when(hashOps)
                .put(eq(CACHE_KEY), anyString(), anyString());

        var original = new ChatAnswer(
                "退款路径是这样的", List.of(new ChatSource(ChatSource.Type.KNOWLEDGE, "kb-1", "帮助中心")), "s-9", false);
        cache.store(AiCallScope.CHAT, USER, "怎么退款？", QUERY_VECTOR, original);

        assertThat(written.get()).isNotNull();
        when(hashOps.entries(CACHE_KEY)).thenReturn(Map.of("f", written.get()));
        var result = cache.lookUp(AiCallScope.CHAT, USER, "怎么退款？", QUERY_VECTOR, ChatAnswer.class);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(original);
    }

    @Test
    @DisplayName("Redis 不可用但向量已算好 -> 仍不动作（缓存 fail-open，问答继续）")
    void redisMissingIsFailOpen() {
        SemanticCacheService cacheNoRedis = new SemanticCacheService(
                mock(ObjectProvider.class),
                embeddingModelProvider,
                aiModelSupport,
                PropertyBindings.bind(AiProperties.class),
                new ObjectMapper(),
                new SemanticCacheMetrics(meterRegistry));

        assertThat(cacheNoRedis.lookUp(AiCallScope.CHAT, USER, "问题", QUERY_VECTOR, ChatAnswer.class))
                .isEmpty();
        cacheNoRedis.store(AiCallScope.CHAT, USER, "问题", QUERY_VECTOR, new ChatAnswer("回答", List.of(), "s", false));
    }

    @Nested
    @DisplayName("命中率指标口径（分母只由真实查找构成）")
    class MetricsTests {

        @Test
        @DisplayName("相似度超阈值 -> hit，未超 -> miss")
        void hitAndMissAreDistinguished() {
            when(redisProvider.getIfAvailable()).thenReturn(redis);
            when(redis.opsForHash()).thenReturn(hashOps);
            when(hashOps.entries(CACHE_KEY)).thenReturn(Map.of("f", CACHED_JSON));

            assertThat(cache.lookUp(AiCallScope.CHAT, USER, "问题", QUERY_VECTOR, ChatAnswer.class))
                    .isPresent();
            assertThat(outcomeCount(Outcome.HIT)).isEqualTo(1d);

            // 与 CACHED_JSON 向量正交（0.99, 0.1, 0）—— 余弦低于 0.92 阈值
            assertThat(cache.lookUp(AiCallScope.CHAT, USER, "问题", List.of(0f, 1f, 0f), ChatAnswer.class))
                    .isEmpty();
            assertThat(outcomeCount(Outcome.MISS)).isEqualTo(1d);
        }

        @Test
        @DisplayName("缓存关闭 / 向量不可用 -> bypass，不进命中率分母")
        void bypassIsExcludedFromDenominator() {
            cache = cache(PropertyBindings.bind(AiProperties.class, "semantic-cache.enabled", "false"));
            assertThat(cache.embedQuery("问题")).isEmpty();
            assertThat(cache.lookUp(AiCallScope.CHAT, USER, "问题", QUERY_VECTOR, ChatAnswer.class))
                    .isEmpty();

            assertThat(outcomeCount(Outcome.BYPASS)).isEqualTo(2d);
            assertThat(outcomeCount(Outcome.MISS)).as("未发生查找，不得记成 miss").isZero();
            assertThat(outcomeCount(Outcome.HIT)).isZero();
        }

        @Test
        @DisplayName("向量化抛异常 -> error 而非 bypass（钱已付却没查成，与主动不查不是一回事）")
        void embeddingFailureCountsAsErrorNotBypass() {
            when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
            when(aiModelSupport.embed(any(), any(), anyString())).thenThrow(new RuntimeException("供应商超时"));

            assertThat(cache.embedQuery("问题")).isEmpty();

            assertThat(outcomeCount(Outcome.ERROR)).isEqualTo(1d);
            assertThat(outcomeCount(Outcome.BYPASS)).isZero();
        }

        @Test
        @DisplayName("Redis 读取抛异常 -> error，fail-open 不记 miss")
        void redisFailureCountsAsErrorNotMiss() {
            when(redisProvider.getIfAvailable()).thenReturn(redis);
            when(redis.opsForHash()).thenThrow(new RuntimeException("redis down"));

            assertThat(cache.lookUp(AiCallScope.CHAT, USER, "问题", QUERY_VECTOR, ChatAnswer.class))
                    .isEmpty();

            assertThat(outcomeCount(Outcome.ERROR)).isEqualTo(1d);
            assertThat(outcomeCount(Outcome.MISS))
                    .as("故障记成 miss 会让人在故障期间误以为阈值该调松")
                    .isZero();
        }

        @Test
        @DisplayName("缓存响应反序列化失败 -> 只记 error，一次查找不出两个结局")
        void deserializationFailureRecordsExactlyOneOutcome() {
            when(redisProvider.getIfAvailable()).thenReturn(redis);
            when(redis.opsForHash()).thenReturn(hashOps);
            // 命中条目但 response 不是合法 JSON —— 语义上已选中却交不出结果
            String corrupt =
                    cachedEntry(List.of(0.99f, 0.1f, 0f), "回答", 1).replace("\"response\":\"", "\"response\":\"{");
            when(hashOps.entries(CACHE_KEY)).thenReturn(Map.of("f", corrupt));

            assertThat(cache.lookUp(AiCallScope.CHAT, USER, "问题", QUERY_VECTOR, ChatAnswer.class))
                    .isEmpty();

            assertThat(outcomeCount(Outcome.ERROR)).isEqualTo(1d);
            assertThat(outcomeCount(Outcome.HIT))
                    .as("先记 hit 再抛会被 catch 补记 error，分子分母同时被污染")
                    .isZero();
        }
    }

    /** 构造一条缓存条目 JSON：向量按 base64 float32 存（与生产写入格式一致）。 */
    private static String cachedEntry(List<Float> vector, String answer, long timestamp) {
        try {
            String response = JSON.writeValueAsString(new ChatAnswer(answer, List.of(), "s", false));
            return JSON.writeValueAsString(Map.of(
                    "vector", SemanticCacheService.encodeVector(vector), "response", response, "timestamp", timestamp));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
