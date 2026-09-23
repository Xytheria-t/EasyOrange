package com.cartethyia.easyorange.ai.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.adapter.outbound.budget.InMemoryTokenBudgetStore;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.port.AiCallLogPort;
import com.cartethyia.easyorange.ai.testsupport.PropertyBindings;
import com.cartethyia.easyorange.ai.testsupport.TestAiModelSupport;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiModelSupport 调用去重工具测试")
class AiModelSupportTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private AiCallLogPort callLogRecorder;

    private AiModelSupport aiModelSupport;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        aiModelSupport = TestAiModelSupport.create(callLogRecorder);
    }

    private static ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Nested
    @DisplayName("callText")
    class CallTextTests {

        @Test
        @DisplayName("system + user 双消息文本生成")
        void callText_success() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("你好"));

            String result = aiModelSupport.callText(chatModel, "system", "user");

            assertThat(result).isEqualTo("你好");
            verify(chatModel).call(any(Prompt.class));
        }

        @Test
        @DisplayName("带 scope 调用 -> 成功时记录日志（scope/model/promptHash/耗时）")
        void callText_withScope_recordsLog() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("你好"));

            String result = aiModelSupport.callText(chatModel, AiCallScope.CHAT, "system", "user");

            assertThat(result).isEqualTo("你好");
            verify(callLogRecorder)
                    .record(
                            eq("CHAT"),
                            anyString(),
                            anyString(),
                            eq("你好"),
                            anyLong(),
                            anyInt(),
                            anyInt(),
                            eq(true),
                            isNull());
        }

        @Test
        @DisplayName("带 scope 调用失败 -> 记录失败日志后重抛异常")
        void callText_withScope_recordsFailure() {
            when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API timeout"));

            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> aiModelSupport.callText(chatModel, AiCallScope.CHAT, "system", "user"))
                    .isInstanceOf(RuntimeException.class);

            verify(callLogRecorder)
                    .record(
                            eq("CHAT"),
                            anyString(),
                            anyString(),
                            isNull(),
                            anyLong(),
                            anyInt(),
                            anyInt(),
                            eq(false),
                            anyString());
        }

        @Test
        @DisplayName("成功调用 -> 响应文本与 scope 落进调用日志")
        void callText_success_recordsCallLog() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("你好"));

            aiModelSupport.callText(chatModel, AiCallScope.CHAT, "system", "user");

            verify(callLogRecorder)
                    .record(
                            eq("CHAT"),
                            anyString(),
                            anyString(),
                            eq("你好"),
                            anyLong(),
                            anyInt(),
                            anyInt(),
                            eq(true),
                            isNull());
        }
    }

    @Nested
    @DisplayName("callJson")
    class CallJsonTests {

        @Test
        @DisplayName("JSON 结构化输出请求并返回文本")
        void callJson_success() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("{\"key\":\"value\"}"));

            String result = aiModelSupport.callJson(chatModel, "system", "user");

            assertThat(result).isEqualTo("{\"key\":\"value\"}");
            verify(chatModel).call(any(Prompt.class));
        }
    }

    @Nested
    @DisplayName("embed")
    class EmbedTests {

        @Test
        @DisplayName("float[] 向量转 List<Float>")
        void embed_convertsArray() {
            when(embeddingModel.embed("iPhone 14")).thenReturn(new float[] {1.0f, 2.5f, -3.0f});

            List<Float> result = aiModelSupport.embed(embeddingModel, "iPhone 14");

            assertThat(result).containsExactly(1.0f, 2.5f, -3.0f);
            verify(embeddingModel).embed("iPhone 14");
        }
    }

    @Nested
    @DisplayName("callJsonAsWithImages")
    class CallJsonAsWithImagesTests {

        @Test
        @DisplayName("多图按 URL 后缀标注 MIME，随提示词交给视觉模型并解析成结构化结果")
        void callJsonAsWithImages_attachesMediaAndParses() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("{\"title\":\"二手相机\"}"));

            Optional<Listing> result = aiModelSupport.callJsonAsWithImages(
                    chatModel,
                    AiCallScope.AUTO_LISTING,
                    "system",
                    "user",
                    List.of("http://example.com/a.png", "http://example.com/b.webp"),
                    Listing.class);

            assertThat(result).isPresent();
            assertThat(result.get().title()).isEqualTo("二手相机");

            var captor = ArgumentCaptor.forClass(Prompt.class);
            verify(chatModel).call(captor.capture());
            var userMessage = (UserMessage) captor.getValue().getInstructions().get(1);
            assertThat(userMessage.getText()).isEqualTo("user");
            assertThat(userMessage.getMedia())
                    .extracting(Media::getMimeType)
                    .containsExactly(Media.Format.IMAGE_PNG, Media.Format.IMAGE_WEBP);
        }

        @Test
        @DisplayName("data URL 从 mime 头解析类型（服务端取图转 base64 后的内联形态，后缀推断对 base64 无效）")
        void callJsonAsWithImages_dataUrlMimeFromHeader() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("{\"title\":\"二手相机\"}"));

            Optional<Listing> result = aiModelSupport.callJsonAsWithImages(
                    chatModel,
                    AiCallScope.AUTO_LISTING,
                    "system",
                    "user",
                    List.of("data:image/png;base64,iVBORw0KGgo="),
                    Listing.class);

            assertThat(result).isPresent();

            var captor = ArgumentCaptor.forClass(Prompt.class);
            verify(chatModel).call(captor.capture());
            var userMessage = (UserMessage) captor.getValue().getInstructions().get(1);
            assertThat(userMessage.getMedia()).extracting(Media::getMimeType).containsExactly(Media.Format.IMAGE_PNG);
        }

        @Test
        @DisplayName("模型输出不可解析 — 返回 empty 由调用方决定语义，不抛异常")
        void callJsonAsWithImages_unparsable() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("{broken}"));

            Optional<Listing> result = aiModelSupport.callJsonAsWithImages(
                    chatModel,
                    AiCallScope.AUTO_LISTING,
                    "system",
                    "user",
                    List.of("http://example.com/a.jpg"),
                    Listing.class);

            assertThat(result).isEmpty();
        }

        /** 只用到 title，够验证反序列化走通即可。 */
        private record Listing(String title) {}
    }

    @Nested
    @DisplayName("预算记账")
    class BudgetAccountingTests {

        private InMemoryTokenBudgetStore budgetStore;
        private AiModelSupport support;

        @BeforeEach
        void setUpBudgetStore() {
            budgetStore = new InMemoryTokenBudgetStore();
            support =
                    TestAiModelSupport.create(callLogRecorder, budgetStore, PropertyBindings.bind(AiProperties.class));
        }

        @Test
        @DisplayName("供应商回报用量 -> 按真实 prompt/completion 记账")
        void recordsRealUsage() {
            when(chatModel.call(any(Prompt.class))).thenReturn(responseWithUsage("回答", 120, 30));

            support.callText(chatModel, AiCallScope.CHAT, "system", "user");

            var usage = budgetStore.getTodayUsage(AiCallScope.CHAT.budgetScenario());
            assertThat(usage).isPresent();
            assertThat(usage.get().inputTokens()).isEqualTo(120);
            assertThat(usage.get().outputTokens()).isEqualTo(30);
            assertThat(usage.get().total()).isEqualTo(150);
        }

        @Test
        @DisplayName("供应商未回报用量 -> 退化为场景上限估算，预算不会静默不累计")
        void fallsBackToConfiguredEstimate() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("回答"));
            var props = PropertyBindings.bind(AiProperties.class, "budget.scenarios.chat.max-tokens-per-call", "1500");
            support = TestAiModelSupport.create(callLogRecorder, budgetStore, props);

            support.callText(chatModel, AiCallScope.CHAT, "system", "user");

            var usage = budgetStore.getTodayUsage(AiCallScope.CHAT.budgetScenario());
            assertThat(usage).isPresent();
            assertThat(usage.get().total()).isEqualTo(1500);
        }

        @Test
        @DisplayName("embedding 供应商回报 usage -> 按真实 prompt token 记账（成本报表不再恒 0）")
        void embedding_recordsReportedUsage() {
            when(embeddingModel.embedForResponse(List.of("二手手机"))).thenReturn(embeddingResponseWithUsage(12));

            List<Float> result = support.embed(embeddingModel, AiCallScope.SEMANTIC, "二手手机");

            assertThat(result).containsExactly(0.5f, -0.25f);
            var usage = budgetStore.getTodayUsage(AiCallScope.SEMANTIC.budgetScenario());
            assertThat(usage).isPresent();
            assertThat(usage.get().inputTokens()).isEqualTo(12);
            assertThat(usage.get().total()).isEqualTo(12);
            verify(callLogRecorder)
                    .record(
                            eq("SEMANTIC"),
                            anyString(),
                            anyString(),
                            isNull(),
                            anyLong(),
                            eq(12),
                            eq(0),
                            eq(true),
                            isNull());
        }

        @Test
        @DisplayName("embedding 未回报 usage -> 仍退化为场景上限估算，预算不会静默不累计")
        void embedding_fallsBackToConfiguredEstimate() {
            when(embeddingModel.embedForResponse(List.of("二手手机")))
                    .thenReturn(new EmbeddingResponse(List.of(new Embedding(new float[] {0.5f}, 0))));
            var props =
                    PropertyBindings.bind(AiProperties.class, "budget.scenarios.semantic.max-tokens-per-call", "500");
            support = TestAiModelSupport.create(callLogRecorder, budgetStore, props);

            support.embed(embeddingModel, AiCallScope.SEMANTIC, "二手手机");

            var usage = budgetStore.getTodayUsage(AiCallScope.SEMANTIC.budgetScenario());
            assertThat(usage).isPresent();
            assertThat(usage.get().total()).isEqualTo(500);
        }

        @Test
        @DisplayName("调用失败不记账（只在成功且有用量可依据时累计）")
        void failure_recordsNothing() {
            when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("provider down"));

            assertThatThrownBy(() -> support.callText(chatModel, AiCallScope.CHAT, "system", "user"))
                    .isInstanceOf(RuntimeException.class);

            assertThat(budgetStore.getTodayUsage(AiCallScope.CHAT.budgetScenario()))
                    .isEmpty();
        }

        @Test
        @DisplayName("场景键与 @TokenBudget(scenario) 对齐：CHAT -> chat / AUTO_LISTING -> auto_listing")
        void scenarioKeysAlignWithBudgetConfig() {
            assertThat(AiCallScope.CHAT.budgetScenario()).isEqualTo("chat");
            assertThat(AiCallScope.AUTO_LISTING.budgetScenario()).isEqualTo("auto_listing");
            assertThat(AiCallScope.SEMANTIC.budgetScenario()).isEqualTo("semantic");
            assertThat(AiCallScope.KNOWLEDGE.budgetScenario()).isEqualTo("knowledge");
        }
    }

    private static ChatResponse responseWithUsage(String text, int promptTokens, int completionTokens) {
        return new ChatResponse(
                List.of(new Generation(new AssistantMessage(text))),
                ChatResponseMetadata.builder()
                        .usage(new DefaultUsage(promptTokens, completionTokens))
                        .build());
    }

    /** embedding 响应夹具：向量 + 供应商回报的 prompt token（completion 对 embedding 恒为 0）。 */
    private static EmbeddingResponse embeddingResponseWithUsage(int promptTokens) {
        return new EmbeddingResponse(
                List.of(new Embedding(new float[] {0.5f, -0.25f}, 0)),
                new EmbeddingResponseMetadata("text-embedding-v3", new DefaultUsage(promptTokens, 0)));
    }
}
