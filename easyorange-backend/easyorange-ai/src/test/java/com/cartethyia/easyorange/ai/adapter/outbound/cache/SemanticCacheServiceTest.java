package com.cartethyia.easyorange.ai.adapter.outbound.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.service.AiModelSupport;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    private static final String CACHED_JSON = """
            {"embedding":[0.99,0.1,0.0],"response":"{\\"answer\\":\\"缓存回答\\",\\"sources\\":[],\\"sessionId\\":\\"s\\"}","timestamp":1}
            """;

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

    private AiProperties aiProperties;
    private SemanticCacheService cache;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        cache = new SemanticCacheService(
                redisProvider, embeddingModelProvider, aiModelSupport, aiProperties, new ObjectMapper());
    }

    @Test
    @DisplayName("相似查询超过阈值 -> 命中返回缓存回答")
    void get_hitOnSimilarQuery() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForHash()).thenReturn(hashOps);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(aiModelSupport.embed(any(), anyString())).thenReturn(List.of(1f, 0f, 0f));
        when(hashOps.entries("eo:ai:semantic:chat")).thenReturn(Map.of("f1", CACHED_JSON));

        Optional<ChatAnswer> result = cache.get(AiCallScope.CHAT, "怎么退款？", ChatAnswer.class);

        assertThat(result).isPresent();
        assertThat(result.get().answer()).isEqualTo("缓存回答");
    }

    @Test
    @DisplayName("不相似查询 -> 未命中")
    void get_missOnDissimilar() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForHash()).thenReturn(hashOps);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(aiModelSupport.embed(any(), anyString())).thenReturn(List.of(1f, 0f, 0f));
        String dissimilar = """
                {"embedding":[0.0,1.0,0.0],"response":"{\\"answer\\":\\"缓存回答\\",\\"sources\\":[],\\"sessionId\\":\\"s\\"}","timestamp":1}
                """;
        when(hashOps.entries("eo:ai:semantic:chat")).thenReturn(Map.of("f1", dissimilar));

        Optional<ChatAnswer> result = cache.get(AiCallScope.CHAT, "怎么退款？", ChatAnswer.class);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("写入 -> 存 (embedding, response) 并刷新 TTL")
    void put() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForHash()).thenReturn(hashOps);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(aiModelSupport.embed(any(), anyString())).thenReturn(List.of(1f, 0f, 0f));
        when(hashOps.size("eo:ai:semantic:chat")).thenReturn(0L);

        cache.put(AiCallScope.CHAT, "怎么退款？", new ChatAnswer("回答", List.of(), "s"));

        verify(hashOps).put(eq("eo:ai:semantic:chat"), anyString(), anyString());
        verify(redis).expire("eo:ai:semantic:chat", Duration.ofHours(24));
    }

    @Test
    @DisplayName("条目超上限 -> 淘汰最旧一条再写入")
    void put_evictsOldest() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForHash()).thenReturn(hashOps);
        when(embeddingModelProvider.getIfAvailable()).thenReturn(embeddingModel);
        when(aiModelSupport.embed(any(), anyString())).thenReturn(List.of(1f, 0f, 0f));
        when(hashOps.size("eo:ai:semantic:chat")).thenReturn(500L);
        String oldEntry = """
                {"embedding":[1.0,0.0,0.0],"response":"{\\"answer\\":\\"旧\\",\\"sources\\":[],\\"sessionId\\":\\"s\\"}","timestamp":100}
                """;
        String newEntry = """
                {"embedding":[1.0,0.0,0.0],"response":"{\\"answer\\":\\"新\\",\\"sources\\":[],\\"sessionId\\":\\"s\\"}","timestamp":200}
                """;
        when(hashOps.entries("eo:ai:semantic:chat")).thenReturn(Map.of("old", oldEntry, "new", newEntry));

        cache.put(AiCallScope.CHAT, "问题", new ChatAnswer("回答", List.of(), "s"));

        verify(hashOps).delete("eo:ai:semantic:chat", "old");
    }

    @Test
    @DisplayName("关闭缓存 -> 直接未命中")
    void disabled() {
        aiProperties.getSemanticCache().setEnabled(false);

        assertThat(cache.get(AiCallScope.CHAT, "问题", ChatAnswer.class)).isEmpty();

        cache.put(AiCallScope.CHAT, "问题", new ChatAnswer("回答", List.of(), "s"));
        verify(redisProvider, org.mockito.Mockito.never()).getIfAvailable();
    }

    @Test
    @DisplayName("Redis 不可用 -> 未命中（fail-open）")
    void noRedis() {
        SemanticCacheService cacheNoRedis = new SemanticCacheService(
                mock(ObjectProvider.class), embeddingModelProvider, aiModelSupport, aiProperties, new ObjectMapper());

        assertThat(cacheNoRedis.get(AiCallScope.CHAT, "问题", ChatAnswer.class)).isEmpty();
    }
}
