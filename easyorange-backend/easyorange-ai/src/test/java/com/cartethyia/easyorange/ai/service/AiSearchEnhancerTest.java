package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.adapter.outbound.AiSearchEnhancerAdapter;
import com.cartethyia.easyorange.ai.port.LlmPort;
import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.framework.cache.RedisCache;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiSearchEnhancerAdapter -> 测试")
class AiSearchEnhancerTest {

    @Mock
    private NaturalLanguageDetector nlDetector;

    @Mock
    private LlmPort llmPort;

    @Mock
    private ProductTagger productTagger;

    @Mock
    private RedisCache redisCache;

    @Mock
    private ObjectProvider<RedisCache> redisCacheProvider;

    private AiSearchEnhancerAdapter enhancer;

    @BeforeEach
    void setUp() {
        lenient().when(redisCacheProvider.getIfAvailable()).thenReturn(redisCache);
        enhancer = new AiSearchEnhancerAdapter(nlDetector, llmPort, productTagger, redisCacheProvider);
    }

    private ProductReadModel product(Long id, String title, BigDecimal price) {
        return new ProductReadModel(
                id, 1L, null, null, null, null,
                title, null, price, price.multiply(BigDecimal.valueOf(2)),
                null, null, null, null, null, null,
                null, null, List.of("img.jpg"), null, null, null, null, null, null, null
        );
    }

    @Nested
    @DisplayName("前置条件检查")
    class PreconditionChecks {

        @Test
        @DisplayName("非自然语言 -> 返回 empty")
        void tryEnhance_notNaturalLanguage() {
            when(nlDetector.isNaturalLanguage("MacBook")).thenReturn(false);

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "MacBook", List.of(product(1L, "MacBook", BigDecimal.valueOf(8000))));

            assertThat(result).isEmpty();
            verifyNoInteractions(llmPort, productTagger, redisCache);
        }

        @Test
        @DisplayName("空商品列表 -> 返回 empty")
        void tryEnhance_emptyProducts() {
            when(nlDetector.isNaturalLanguage("找电脑")).thenReturn(true);

            Optional<AiEnhancement> result = enhancer.tryEnhance("找电脑", List.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(llmPort, productTagger);
        }

        @Test
        @DisplayName("null 商品列表 -> 返回 empty")
        void tryEnhance_nullProducts() {
            when(nlDetector.isNaturalLanguage("找电脑")).thenReturn(true);

            Optional<AiEnhancement> result = enhancer.tryEnhance("找电脑", null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("缓存命中")
    class CacheHitTests {

        @Test
        @DisplayName("Redis 有缓存 -> 直接返回，不调用 LLM")
        void tryEnhance_cacheHit() {
            when(nlDetector.isNaturalLanguage("找便宜手机")).thenReturn(true);
            AiEnhancement cached = new AiEnhancement(
                    "想找低价智能手机", Map.of(), "市场均价2000左右", List.of()
            );
            when(redisCache.get(anyString(), eq(AiEnhancement.class))).thenReturn(cached);

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "找便宜手机", List.of(product(1L, "手机", BigDecimal.valueOf(1500))));

            assertThat(result).isPresent().get().isEqualTo(cached);
            verifyNoInteractions(llmPort, productTagger);
        }

        @Test
        @DisplayName("Redis 缓存未配置 -> 正常走增强流程")
        void tryEnhance_noRedisConfigured() {
            when(redisCacheProvider.getIfAvailable()).thenReturn(null);
            enhancer = new AiSearchEnhancerAdapter(nlDetector, llmPort, productTagger, redisCacheProvider);
            when(nlDetector.isNaturalLanguage("找电脑")).thenReturn(true);
            when(productTagger.tagProducts(anyList())).thenReturn(Map.of(1L, List.of()));
            when(llmPort.generateText(anyString(), anyString())).thenReturn("想找电脑");

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "找电脑", List.of(product(1L, "笔记本", BigDecimal.valueOf(4000))));

            assertThat(result).isPresent();
            verify(llmPort, atLeastOnce()).generateText(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("正常增强流程")
    class NormalEnhancementTests {

        @Test
        @DisplayName("全部子任务成功 -> 返回完整 AiEnhancement")
        void tryEnhance_allSuccess() {
            when(nlDetector.isNaturalLanguage("推荐个5000的笔记本")).thenReturn(true);
            when(redisCache.get(anyString(), eq(AiEnhancement.class))).thenReturn(null);
            when(llmPort.generateText(contains("导购助手"), eq("推荐个5000的笔记本")))
                    .thenReturn("想找5000元左右的笔记本电脑");
            when(productTagger.tagProducts(anyList()))
                    .thenReturn(Map.of(1L, List.of("💰超值")));
            when(llmPort.generateText(contains("市场分析"), anyString()))
                    .thenReturn("当前二手笔记本均价约4800元，性价比不错");
            when(llmPort.generateText(contains("追问"), eq("推荐个5000的笔记本")))
                    .thenReturn("有游戏需求吗,需要轻薄吗");

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "推荐个5000的笔记本",
                    List.of(product(1L, "MacBook Air M1", BigDecimal.valueOf(4200)))
            );

            assertThat(result).isPresent();
            AiEnhancement enhancement = result.get();
            assertThat(enhancement.intentExplanation()).isEqualTo("想找5000元左右的笔记本电脑");
            assertThat(enhancement.productTags()).containsKey(1L);
            assertThat(enhancement.marketAnalysis()).isEqualTo("当前二手笔记本均价约4800元，性价比不错");
            assertThat(enhancement.suggestedQuestions())
                    .hasSize(2)
                    .containsExactly("有游戏需求吗", "需要轻薄吗");
            verify(redisCache).set(anyString(), any(AiEnhancement.class), eq(5L), any());
        }

        @Test
        @DisplayName("仅 tags 有结果 -> 返回 tags 数据")
        void tryEnhance_onlyTagsSucceed() {
            when(nlDetector.isNaturalLanguage("找个手机")).thenReturn(true);
            when(redisCache.get(anyString(), eq(AiEnhancement.class))).thenReturn(null);
            when(llmPort.generateText(anyString(), anyString())).thenReturn(null);
            when(productTagger.tagProducts(anyList()))
                    .thenReturn(Map.of(1L, List.of("💰超值", "📸实拍")));

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "找个手机", List.of(product(1L, "iPhone", BigDecimal.valueOf(2500))));

            assertThat(result).isPresent();
            assertThat(result.get().productTags()).containsKey(1L);
            assertThat(result.get().intentExplanation()).isNull();
        }

        @Test
        @DisplayName("所有任务都返回空 -> 返回 empty")
        void tryEnhance_allEmpty() {
            when(nlDetector.isNaturalLanguage("随便看看")).thenReturn(true);
            when(redisCache.get(anyString(), eq(AiEnhancement.class))).thenReturn(null);
            when(llmPort.generateText(anyString(), anyString())).thenReturn(null);
            when(productTagger.tagProducts(anyList())).thenReturn(Map.of());

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "随便看看", List.of(product(1L, "商品A", BigDecimal.valueOf(100))));

            assertThat(result).isEmpty();
            verify(redisCache, never()).set(anyString(), any(), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("容错降级")
    class FallbackTests {

        @Test
        @DisplayName("LLM 抛异常 -> 降级返回已有结果")
        void tryEnhance_llmException_fallbackToTags() {
            when(nlDetector.isNaturalLanguage("找东西")).thenReturn(true);
            when(redisCache.get(anyString(), eq(AiEnhancement.class))).thenReturn(null);
            when(llmPort.generateText(anyString(), anyString()))
                    .thenThrow(new RuntimeException("API timeout"));
            when(productTagger.tagProducts(anyList()))
                    .thenReturn(Map.of(1L, List.of("⭐信用优")));

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "找东西", List.of(product(1L, "商品X", BigDecimal.valueOf(999))));

            assertThat(result).isPresent();
            assertThat(result.get().productTags()).containsKey(1L);
            assertThat(result.get().intentExplanation()).isNull();
        }
    }
}