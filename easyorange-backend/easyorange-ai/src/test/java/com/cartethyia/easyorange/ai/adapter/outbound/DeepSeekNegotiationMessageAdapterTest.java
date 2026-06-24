package com.cartethyia.easyorange.ai.adapter.outbound;

import com.cartethyia.easyorange.ai.port.LlmPort;
import com.cartethyia.easyorange.product.domain.port.NegotiationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeepSeekNegotiationMessageAdapter 测试")
class DeepSeekNegotiationMessageAdapterTest {

    @Mock
    private LlmPort llmPort;

    @Captor
    private ArgumentCaptor<String> systemPromptCaptor;

    @Captor
    private ArgumentCaptor<String> userMessageCaptor;

    private DeepSeekNegotiationMessageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DeepSeekNegotiationMessageAdapter(llmPort);
    }

    @Nested
    @DisplayName("ACCEPT 决策")
    class AcceptTests {

        private static final NegotiationContext ACCEPT_CONTEXT = new NegotiationContext(
                "ACCEPT",
                new BigDecimal("100"),
                null,
                "出价达到底价，直接成交",
                "测试商品",
                new BigDecimal("100")
        );

        @Test
        @DisplayName("调用 LLM 生成接受话术")
        void shouldCallLlmAndReturnResult() {
            when(llmPort.generateText(anyString(), anyString())).thenReturn("好的，¥100成交！请尽快付款~");

            String result = adapter.generateMessage(ACCEPT_CONTEXT);

            assertThat(result).isEqualTo("好的，¥100成交！请尽快付款~");
        }

        @Test
        @DisplayName("LLM 异常时返回兜底接受话术")
        void shouldFallbackWhenLlmFails() {
            when(llmPort.generateText(anyString(), anyString())).thenThrow(new RuntimeException("API不可用"));

            String result = adapter.generateMessage(ACCEPT_CONTEXT);

            assertThat(result).isEqualTo("好的，¥100成交！请尽快付款哦~");
        }

        @Test
        @DisplayName("传递正确的 system prompt 和 user message")
        void shouldPassCorrectPrompt() {
            when(llmPort.generateText(anyString(), anyString())).thenReturn("OK");
            adapter.generateMessage(ACCEPT_CONTEXT);

            verify(llmPort).generateText(systemPromptCaptor.capture(), userMessageCaptor.capture());
            assertThat(systemPromptCaptor.getValue()).contains("AI议价助手");
            assertThat(userMessageCaptor.getValue()).contains("测试商品");
            assertThat(userMessageCaptor.getValue()).contains("¥100");
            assertThat(userMessageCaptor.getValue()).contains("接受");
        }
    }

    @Nested
    @DisplayName("COUNTER 决策")
    class CounterTests {

        private static final NegotiationContext COUNTER_CONTEXT = new NegotiationContext(
                "COUNTER",
                null,
                new BigDecimal("95"),
                "出价接近底价，还价至底价95%",
                "测试商品",
                new BigDecimal("80")
        );

        @Test
        @DisplayName("调用 LLM 生成还价话术")
        void shouldCallLlmAndReturnResult() {
            when(llmPort.generateText(anyString(), anyString())).thenReturn("¥80有点低了，¥95可以吗？");

            String result = adapter.generateMessage(COUNTER_CONTEXT);

            assertThat(result).isEqualTo("¥80有点低了，¥95可以吗？");
        }

        @Test
        @DisplayName("LLM 异常时返回兜底还价话术")
        void shouldFallbackWhenLlmFails() {
            when(llmPort.generateText(anyString(), anyString())).thenThrow(new RuntimeException("超时"));

            String result = adapter.generateMessage(COUNTER_CONTEXT);

            assertThat(result).isEqualTo("¥80有点低啦，¥95可以吗？");
        }

        @Test
        @DisplayName("传递正确的 system prompt 和 user message")
        void shouldPassCorrectPrompt() {
            when(llmPort.generateText(anyString(), anyString())).thenReturn("OK");
            adapter.generateMessage(COUNTER_CONTEXT);

            verify(llmPort).generateText(systemPromptCaptor.capture(), userMessageCaptor.capture());
            assertThat(systemPromptCaptor.getValue()).contains("AI议价助手");
            assertThat(userMessageCaptor.getValue()).contains("还价¥95");
            assertThat(userMessageCaptor.getValue()).contains("还价至底价95%");
        }
    }

    @Nested
    @DisplayName("REJECT 决策")
    class RejectTests {

        private static final NegotiationContext REJECT_CONTEXT = new NegotiationContext(
                "REJECT",
                null,
                null,
                "出价过低",
                "测试商品",
                new BigDecimal("50")
        );

        @Test
        @DisplayName("调用 LLM 生成拒绝话术")
        void shouldCallLlmAndReturnResult() {
            when(llmPort.generateText(anyString(), anyString())).thenReturn("抱歉，¥50太低了~");

            String result = adapter.generateMessage(REJECT_CONTEXT);

            assertThat(result).isEqualTo("抱歉，¥50太低了~");
        }

        @Test
        @DisplayName("LLM 异常时返回兜底拒绝话术")
        void shouldFallbackWhenLlmFails() {
            when(llmPort.generateText(anyString(), anyString())).thenThrow(new RuntimeException("LLM错误"));

            String result = adapter.generateMessage(REJECT_CONTEXT);

            assertThat(result).isEqualTo("抱歉，¥50太低啦，再考虑考虑？");
        }

        @Test
        @DisplayName("传递正确的 system prompt 和 user message")
        void shouldPassCorrectPrompt() {
            when(llmPort.generateText(anyString(), anyString())).thenReturn("OK");
            adapter.generateMessage(REJECT_CONTEXT);

            verify(llmPort).generateText(systemPromptCaptor.capture(), userMessageCaptor.capture());
            assertThat(systemPromptCaptor.getValue()).contains("AI议价助手");
            assertThat(userMessageCaptor.getValue()).contains("拒绝");
            assertThat(userMessageCaptor.getValue()).contains("¥50");
        }
    }
}
