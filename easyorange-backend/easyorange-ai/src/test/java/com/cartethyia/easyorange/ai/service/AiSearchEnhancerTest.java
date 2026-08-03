package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.adapter.outbound.AiSearchEnhancerAdapter;
import com.cartethyia.easyorange.common.dto.AiEnhancement;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
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
    private ChatModel chatModel;

    @Mock
    private ProductTagger productTagger;

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private ValueOperations<Object, Object> valueOps;

    @Mock
    private ObjectProvider<RedisTemplate<Object, Object>> redisTemplateProvider;

    private AiSearchEnhancerAdapter enhancer;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        enhancer = new AiSearchEnhancerAdapter(nlDetector, chatModel, productTagger, redisTemplateProvider);
    }

    private ProductReadModel product(String id, String title, BigDecimal price) {
        return new ProductReadModel(
                id, "1", null, null, null, null,
                title, null, price, price.multiply(BigDecimal.valueOf(2)),
                null, null, null, null, null, null,
                null, null, List.of("img.jpg"), null, null, null
        );
    }

    private static ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    /**
     * Prompt 首条消息（system）包含指定片段的匹配器 — 区分同一 mock 上的多个 LLM 调用。
     */
    private static Prompt withSystemContaining(String fragment) {
        return argThat(p -> {
            if (p == null) {
                return false;
            }
            List<Message> instructions = p.getInstructions();
            if (instructions == null || instructions.isEmpty()) {
                return false;
            }
            String text = instructions.get(0).getText();
            return text != null && text.contains(fragment);
        });
    }

    @Nested
    @DisplayName("前置条件检查")
    class PreconditionChecks {

        @Test
        @DisplayName("非自然语言 -> 返回 empty")
        void tryEnhance_notNaturalLanguage() {
            when(nlDetector.isNaturalLanguage("MacBook")).thenReturn(false);

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "MacBook", List.of(product("1", "MacBook", BigDecimal.valueOf(8000))));

            assertThat(result).isEmpty();
            verifyNoInteractions(chatModel, productTagger, redisTemplate);
        }

        @Test
        @DisplayName("空商品列表 -> 返回 empty")
        void tryEnhance_emptyProducts() {
            when(nlDetector.isNaturalLanguage("找电脑")).thenReturn(true);

            Optional<AiEnhancement> result = enhancer.tryEnhance("找电脑", List.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(chatModel, productTagger);
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
            when(valueOps.get(anyString())).thenReturn(cached);

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "找便宜手机", List.of(product("1", "手机", BigDecimal.valueOf(1500))));

            assertThat(result).isPresent().get().isEqualTo(cached);
            verifyNoInteractions(chatModel, productTagger);
        }

        @Test
        @DisplayName("Redis 缓存未配置 -> 正常走增强流程")
        void tryEnhance_noRedisConfigured() {
            when(redisTemplateProvider.getIfAvailable()).thenReturn(null);
            enhancer = new AiSearchEnhancerAdapter(nlDetector, chatModel, productTagger, redisTemplateProvider);
            when(nlDetector.isNaturalLanguage("找电脑")).thenReturn(true);
            when(productTagger.tagProducts(anyList())).thenReturn(Map.of("1", List.of()));
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("想找电脑"));

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "找电脑", List.of(product("1", "笔记本", BigDecimal.valueOf(4000))));

            assertThat(result).isPresent();
            verify(chatModel, atLeastOnce()).call(any(Prompt.class));
        }
    }

    @Nested
    @DisplayName("正常增强流程")
    class NormalEnhancementTests {

        @Test
        @DisplayName("全部子任务成功 -> 返回完整 AiEnhancement")
        void tryEnhance_allSuccess() {
            when(nlDetector.isNaturalLanguage("推荐个5000的笔记本")).thenReturn(true);
            when(valueOps.get(anyString())).thenReturn(null);
            when(chatModel.call(withSystemContaining("导购助手")))
                    .thenReturn(textResponse("想找5000元左右的笔记本电脑"));
            when(productTagger.tagProducts(anyList()))
                    .thenReturn(Map.of("1", List.of("💰超值")));
            when(chatModel.call(withSystemContaining("市场分析")))
                    .thenReturn(textResponse("当前在管笔记本均价约4800元，性价比不错"));
            when(chatModel.call(withSystemContaining("追问")))
                    .thenReturn(textResponse("有游戏需求吗,需要轻薄吗"));

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "推荐个5000的笔记本",
                    List.of(product("1", "MacBook Air M1", BigDecimal.valueOf(4200)))
            );

            assertThat(result).isPresent();
            AiEnhancement enhancement = result.get();
            assertThat(enhancement.intentExplanation()).isEqualTo("想找5000元左右的笔记本电脑");
            assertThat(enhancement.productTags()).containsKey("1");
            assertThat(enhancement.marketAnalysis()).isEqualTo("当前在管笔记本均价约4800元，性价比不错");
            assertThat(enhancement.suggestedQuestions())
                    .hasSize(2)
                    .containsExactly("有游戏需求吗", "需要轻薄吗");
            verify(valueOps).set(anyString(), any(AiEnhancement.class), eq(5L), any());
        }

        @Test
        @DisplayName("仅 tags 有结果 -> 返回 tags 数据")
        void tryEnhance_onlyTagsSucceed() {
            when(nlDetector.isNaturalLanguage("找个手机")).thenReturn(true);
            when(valueOps.get(anyString())).thenReturn(null);
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse(null));
            when(productTagger.tagProducts(anyList()))
                    .thenReturn(Map.of("1", List.of("💰超值", "📸实拍")));

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "找个手机", List.of(product("1", "iPhone", BigDecimal.valueOf(2500))));

            assertThat(result).isPresent();
            assertThat(result.get().productTags()).containsKey("1");
            assertThat(result.get().intentExplanation()).isNull();
        }

        @Test
        @DisplayName("所有任务都返回空 -> 返回 empty")
        void tryEnhance_allEmpty() {
            when(nlDetector.isNaturalLanguage("随便看看")).thenReturn(true);
            when(valueOps.get(anyString())).thenReturn(null);
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse(null));
            when(productTagger.tagProducts(anyList())).thenReturn(Map.of());

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "随便看看", List.of(product("1", "商品A", BigDecimal.valueOf(100))));

            assertThat(result).isEmpty();
            verify(valueOps, never()).set(anyString(), any(), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("容错降级")
    class FallbackTests {

        @Test
        @DisplayName("LLM 抛异常 -> 降级返回已有结果")
        void tryEnhance_llmException_fallbackToTags() {
            when(nlDetector.isNaturalLanguage("找东西")).thenReturn(true);
            when(valueOps.get(anyString())).thenReturn(null);
            when(chatModel.call(any(Prompt.class)))
                    .thenThrow(new RuntimeException("API timeout"));
            when(productTagger.tagProducts(anyList()))
                    .thenReturn(Map.of("1", List.of("⭐信用优")));

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "找东西", List.of(product("1", "商品X", BigDecimal.valueOf(999))));

            assertThat(result).isPresent();
            assertThat(result.get().productTags()).containsKey("1");
            assertThat(result.get().intentExplanation()).isNull();
        }
    }
}
