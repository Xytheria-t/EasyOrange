package com.cartethyia.easyorange.ai.application.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;

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

            String result = aiModelSupport.callText(chatModel, AiCallScope.QA, "system", "user");

            assertThat(result).isEqualTo("你好");
            verify(callLogRecorder).record(eq("QA"), anyString(), anyString(), eq("你好"), anyLong(), eq(true), isNull());
        }

        @Test
        @DisplayName("带 scope 调用失败 -> 记录失败日志后重抛异常")
        void callText_withScope_recordsFailure() {
            when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API timeout"));

            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> aiModelSupport.callText(chatModel, AiCallScope.QA, "system", "user"))
                    .isInstanceOf(RuntimeException.class);

            verify(callLogRecorder)
                    .record(eq("QA"), anyString(), anyString(), isNull(), anyLong(), eq(false), anyString());
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
    @DisplayName("analyzeImages")
    class AnalyzeImagesTests {

        @Test
        @DisplayName("多图 Media 随提示词交给视觉模型")
        void analyzeImages_success() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("看到一个九成新的手机"));

            String result = aiModelSupport.analyzeImages(
                    chatModel,
                    AiCallScope.AUTO_LISTING,
                    List.of("http://example.com/a.jpg", "http://example.com/b.jpg"),
                    "请描述图片内容");

            assertThat(result).isEqualTo("看到一个九成新的手机");
            verify(chatModel).call(any(Prompt.class));
        }
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
            assertThat(AiCallScope.QA.budgetScenario()).isEqualTo("qa");
        }
    }

    private static ChatResponse responseWithUsage(String text, int promptTokens, int completionTokens) {
        return new ChatResponse(
                List.of(new Generation(new AssistantMessage(text))),
                ChatResponseMetadata.builder()
                        .usage(new DefaultUsage(promptTokens, completionTokens))
                        .build());
    }
}
