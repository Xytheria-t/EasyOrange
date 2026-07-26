package com.cartethyia.easyorange.ai.adapter;

import com.cartethyia.easyorange.ai.adapter.dto.DeepSeekRequest;
import com.cartethyia.easyorange.ai.adapter.dto.DeepSeekResponse;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.metrics.AiMetricsService;
import com.cartethyia.easyorange.ai.port.LlmPort;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeepSeekLlmAdapter 测试")
class DeepSeekLlmAdapterTest {

    @Mock
    private RestClient deepseekRestClient;

    @Mock
    private AiProperties aiProperties;

    @Mock
    private AiProperties.DeepSeek deepSeekProps;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private AiMetricsService aiMetricsService;

    @Captor
    private ArgumentCaptor<DeepSeekRequest> requestCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    private LlmPort adapter;

    @BeforeEach
    void setUp() {
        lenient().when(aiProperties.getDeepseek()).thenReturn(deepSeekProps);
        lenient().when(deepSeekProps.getModel()).thenReturn("deepseek-chat");
        lenient().when(aiMetricsService.startTimer()).thenReturn(Timer.start(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
        lenient().doNothing().when(aiMetricsService).recordLlmDuration(anyString(), any(), anyString());
        adapter = new DeepSeekLlmAdapter(deepseekRestClient, aiProperties, aiMetricsService);
    }

    private void stubChatChain(DeepSeekResponse mockResponse) {
        when(deepseekRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/v1/chat/completions")).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(DeepSeekRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(DeepSeekResponse.class)).thenReturn(mockResponse);
    }

    private void stubEmbeddingChain() {
        when(deepseekRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/v1/embeddings")).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Nested
    @DisplayName("generateText")
    class GenerateTextTests {

        @Test
        @DisplayName("正常返回 LLM 生成的文本")
        void success() {
            DeepSeekResponse mockResponse = mock(DeepSeekResponse.class);
            when(mockResponse.getFirstChoiceContent()).thenReturn("这是一个生成的文本回复。");
            stubChatChain(mockResponse);

            String result = adapter.generateText("system prompt", "user message");

            assertThat(result).isEqualTo("这是一个生成的文本回复。");
        }

        @Test
        @DisplayName("响应为 null 时返回 null")
        void nullResponse() {
            stubChatChain(null);

            String result = adapter.generateText("system prompt", "user message");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("choices 为空列表时返回 null")
        void emptyChoices() {
            DeepSeekResponse mockResponse = mock(DeepSeekResponse.class);
            when(mockResponse.getFirstChoiceContent()).thenReturn(null);
            stubChatChain(mockResponse);

            String result = adapter.generateText("system prompt", "user message");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("generateTextWithJson")
    class GenerateTextWithJsonTests {

        @Test
        @DisplayName("正常返回 JSON 格式文本")
        void success() {
            String expectedJson = "{\"key\":\"value\",\"number\":42}";
            DeepSeekResponse mockResponse = mock(DeepSeekResponse.class);
            when(mockResponse.getFirstChoiceContent()).thenReturn(expectedJson);
            stubChatChain(mockResponse);

            String result = adapter.generateTextWithJson("system prompt", "user message");

            assertThat(result).isEqualTo(expectedJson);
        }

        @Test
        @DisplayName("响应为 null 时返回 null")
        void nullResponse() {
            stubChatChain(null);

            String result = adapter.generateTextWithJson("system prompt", "user message");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("metrics — LLM 延迟记录")
    class MetricsTests {

        @Test
        @DisplayName("LLM 调用成功时记录 success 延迟")
        void llmSuccessRecordsDuration() {
            DeepSeekResponse mockResponse = mock(DeepSeekResponse.class);
            when(mockResponse.getFirstChoiceContent()).thenReturn("result");
            stubChatChain(mockResponse);
            adapter.generateText("sys", "user");
            verify(aiMetricsService).recordLlmDuration(eq("REVIEW"), any(), eq("success"));
        }

        @Test
        @DisplayName("LLM 调用异常时记录 error 延迟")
        void llmErrorRecordsDuration() {
            when(deepseekRestClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri("/v1/chat/completions")).thenReturn(requestBodySpec);
            when(requestBodySpec.body(any(DeepSeekRequest.class))).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(DeepSeekResponse.class)).thenThrow(new RuntimeException("API error"));
            assertThatThrownBy(() -> adapter.generateText("sys", "user"));
            verify(aiMetricsService).recordLlmDuration(eq("REVIEW"), any(), eq("error"));
        }
    }

    @Nested
    @DisplayName("generateEmbedding")
    class GenerateEmbeddingTests {

        @Test
        @DisplayName("响应为 null 时返回空列表")
        void nullResponseReturnsEmptyList() {
            stubEmbeddingChain();
            when(responseSpec.body((Class<?>) any())).thenReturn(null);

            List<Float> result = adapter.generateEmbedding("test text");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("请求体构建正确 — model 为 deepseek-embedding")
        void usesCorrectModelInRequest() {
            stubEmbeddingChain();
            when(responseSpec.body((Class<?>) any())).thenReturn(null);

            adapter.generateEmbedding("search keyword");

            verify(requestBodyUriSpec).uri("/v1/embeddings");
        }
    }
}
