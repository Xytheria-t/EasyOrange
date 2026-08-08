package com.cartethyia.easyorange.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.adapter.outbound.AiCallLogRecorder;
import com.cartethyia.easyorange.ai.dto.QaRequest;
import com.cartethyia.easyorange.ai.dto.QaResponse;
import com.cartethyia.easyorange.ai.prompt.TestPromptRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiQaService 测试")
class AiQaServiceTest {

    @Mock
    private ChatModel chatModel;

    private AiQaService service;

    @BeforeEach
    void setUp() {
        service =
                new AiQaService(chatModel, new TestPromptRegistry(), new AiModelSupport(mock(AiCallLogRecorder.class)));
    }

    private QaRequest createRequest(String question) {
        return new QaRequest("1", question, "iPhone 14", "99新，使用3个月", "手机数码", "¥4500", "九五新", "张三", "高");
    }

    private static ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Nested
    @DisplayName("answerQuestion")
    class AnswerQuestionTests {

        @Test
        @DisplayName("正常问答流程 — 返回有效回答")
        void answerQuestion_success() {
            QaRequest request = createRequest("这个手机是正品吗？");
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("是正品，有官方购买凭证。"));

            QaResponse response = service.answerQuestion(request);

            assertThat(response.answer()).isEqualTo("是正品，有官方购买凭证。");
            assertThat(response.confidence()).isTrue();
            verify(chatModel).call(any(Prompt.class));
        }

        @Test
        @DisplayName("LLM 返回空白时返回 AI 服务不可用信息")
        void answerQuestion_blankResponse() {
            QaRequest request = createRequest("这个多少钱？");
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("   "));

            QaResponse response = service.answerQuestion(request);

            assertThat(response.answer()).isEqualTo("AI服务暂时不可用");
            assertThat(response.confidence()).isFalse();
        }

        @Test
        @DisplayName("LLM 返回 null 时返回 AI 服务不可用信息")
        void answerQuestion_nullResponse() {
            QaRequest request = createRequest("包邮吗？");
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse(null));

            QaResponse response = service.answerQuestion(request);

            assertThat(response.answer()).isEqualTo("AI服务暂时不可用");
            assertThat(response.confidence()).isFalse();
        }

        @Test
        @DisplayName("LLM 抛出异常时返回 AI 服务不可用信息")
        void answerQuestion_exception() {
            QaRequest request = createRequest("有货吗？");
            when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API timeout"));

            QaResponse response = service.answerQuestion(request);

            assertThat(response.answer()).isEqualTo("AI服务暂时不可用");
            assertThat(response.confidence()).isFalse();
        }
    }
}
