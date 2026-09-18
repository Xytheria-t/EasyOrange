package com.cartethyia.easyorange.ai.adapter.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.adapter.outbound.tool.*;
import com.cartethyia.easyorange.ai.application.service.NaturalLanguageDetector;
import com.cartethyia.easyorange.ai.application.service.ProductTagger;
import com.cartethyia.easyorange.ai.testsupport.TestAiModelSupport;
import com.cartethyia.easyorange.ai.testsupport.TestPromptRegistry;
import com.cartethyia.easyorange.common.dto.AiEnhancement;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.math.BigDecimal;
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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiSearchEnhancerAdapter -> 测试")
class AiSearchEnhancerTest {

    /** 与生产默认值一致；用例里模型是 mock（立即返回），总超时值不影响断言。 */
    private static final int TIMEOUT_SECONDS = 5;

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
        enhancer = new AiSearchEnhancerAdapter(nlDetector, buildRegistry(), redisTemplateProvider, TIMEOUT_SECONDS);
    }

    private SearchToolRegistry buildRegistry() {
        return new SearchToolRegistry(List.of(
                new IntentDetectionTool(chatModel, TestAiModelSupport.create(), new TestPromptRegistry()),
                new ProductTaggingTool(productTagger),
                new MarketAnalysisTool(),
                new QuestionSuggestionTool()));
    }

    private ProductReadModel product(String id, String title, BigDecimal price) {
        return new ProductReadModel(
                id,
                "1",
                null,
                null,
                null,
                null,
                title,
                null,
                price,
                price.multiply(BigDecimal.valueOf(2)),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of("img.jpg"),
                null,
                null,
                null);
    }

    private static ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    /**
     * Prompt 首条消息（system）包含指定片段的匹配器 — 区分同一 mock 上的多个 LLM 调用。
     * <p>
     * 匹配片段用模板名（{@link TestPromptRegistry} 的 stub 正文里带着名字），
     * 断言的是「哪路工具挑了哪个 prompt」，不是 prompt 正文本身。
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

            Optional<AiEnhancement> result =
                    enhancer.tryEnhance("MacBook", List.of(product("1", "MacBook", BigDecimal.valueOf(8000))));

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
            AiEnhancement cached = new AiEnhancement("想找低价智能手机", Map.of(), "市场均价2000左右", List.of());
            when(valueOps.get(anyString())).thenReturn(cached);

            Optional<AiEnhancement> result =
                    enhancer.tryEnhance("找便宜手机", List.of(product("1", "手机", BigDecimal.valueOf(1500))));

            assertThat(result).isPresent().get().isEqualTo(cached);
            verifyNoInteractions(chatModel, productTagger);
        }

        @Test
        @DisplayName("Redis 缓存未配置 -> 正常走增强流程")
        void tryEnhance_noRedisConfigured() {
            when(redisTemplateProvider.getIfAvailable()).thenReturn(null);
            enhancer = new AiSearchEnhancerAdapter(nlDetector, buildRegistry(), redisTemplateProvider, TIMEOUT_SECONDS);
            when(nlDetector.isNaturalLanguage("找电脑")).thenReturn(true);
            when(productTagger.tagProducts(anyList())).thenReturn(Map.of("1", List.of()));
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("想找电脑"));

            Optional<AiEnhancement> result =
                    enhancer.tryEnhance("找电脑", List.of(product("1", "笔记本", BigDecimal.valueOf(4000))));

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
            when(chatModel.call(withSystemContaining("search_intent_system")))
                    .thenReturn(textResponse("想找5000元左右的笔记本电脑"));
            when(productTagger.tagProducts(anyList())).thenReturn(Map.of("1", List.of("💰超值")));

            Optional<AiEnhancement> result = enhancer.tryEnhance(
                    "推荐个5000的笔记本", List.of(product("1", "MacBook Air M1", BigDecimal.valueOf(4200))));

            assertThat(result).isPresent();
            AiEnhancement enhancement = result.get();
            assertThat(enhancement.intentExplanation()).isEqualTo("想找5000元左右的笔记本电脑");
            assertThat(enhancement.productTags()).containsKey("1");
            // 后两路已是规则实现：均价/区间由价格算出，追问由关键词与价格下限派生
            assertThat(enhancement.marketAnalysis()).isEqualTo("当前 1 件在售，均价 ¥4200，均为 ¥4200");
            assertThat(enhancement.suggestedQuestions()).containsExactly("「推荐个5000的笔记本」里哪件性价比最高？", "预算 ¥4200 以内能买到什么？");
            verify(valueOps).set(anyString(), any(AiEnhancement.class), eq(5L), any());
            // 4 路工具里只剩意图识别打模型 —— 一次搜索恰好一次 LLM 调用（此前是 3 次）
            verify(chatModel, times(1)).call(any(Prompt.class));
        }

        @Test
        @DisplayName("仅 tags 有结果 -> 返回 tags 数据")
        void tryEnhance_onlyTagsSucceed() {
            when(nlDetector.isNaturalLanguage("找个手机")).thenReturn(true);
            when(valueOps.get(anyString())).thenReturn(null);
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse(null));
            when(productTagger.tagProducts(anyList())).thenReturn(Map.of("1", List.of("💰超值", "📸实拍")));

            Optional<AiEnhancement> result =
                    enhancer.tryEnhance("找个手机", List.of(product("1", "iPhone", BigDecimal.valueOf(2500))));

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

            Optional<AiEnhancement> result =
                    enhancer.tryEnhance("随便看看", List.of(product("1", "商品A", BigDecimal.valueOf(100))));

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
            when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API timeout"));
            when(productTagger.tagProducts(anyList())).thenReturn(Map.of("1", List.of("💰超值")));

            Optional<AiEnhancement> result =
                    enhancer.tryEnhance("找东西", List.of(product("1", "商品X", BigDecimal.valueOf(999))));

            assertThat(result).isPresent();
            assertThat(result.get().productTags()).containsKey("1");
            assertThat(result.get().intentExplanation()).isNull();
        }

        @Test
        @DisplayName("降级结果不写缓存（抖动不被固化成 5 分钟正常结果）")
        void tryEnhance_degraded_doesNotCache() {
            when(nlDetector.isNaturalLanguage("找东西")).thenReturn(true);
            when(valueOps.get(anyString())).thenReturn(null);
            when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API timeout"));
            when(productTagger.tagProducts(anyList())).thenReturn(Map.of("1", List.of("💰超值")));

            enhancer.tryEnhance("找东西", List.of(product("1", "商品X", BigDecimal.valueOf(999))));

            verify(valueOps, never()).set(anyString(), any(), anyLong(), any());
        }

        @Test
        @DisplayName("意外异常不越过 Port 边界（调用方无兜底，逃逸即检索接口失败）")
        void tryEnhance_unexpectedFailure_neverThrows() {
            when(nlDetector.isNaturalLanguage("找东西")).thenThrow(new IllegalStateException("detector broken"));

            Optional<AiEnhancement> result = assertDoesNotThrow(
                    () -> enhancer.tryEnhance("找东西", List.of(product("1", "商品X", BigDecimal.valueOf(999)))));

            assertThat(result).isEmpty();
        }
    }
}
