package com.cartethyia.easyorange.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
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
import org.springframework.ai.embedding.EmbeddingModel;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiModelSupport 调用去重工具测试")
class AiModelSupportTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private EmbeddingModel embeddingModel;

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

            String result = AiModelSupport.callText(chatModel, "system", "user");

            assertThat(result).isEqualTo("你好");
            verify(chatModel).call(any(Prompt.class));
        }
    }

    @Nested
    @DisplayName("callJson")
    class CallJsonTests {

        @Test
        @DisplayName("JSON 结构化输出请求并返回文本")
        void callJson_success() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("{\"key\":\"value\"}"));

            String result = AiModelSupport.callJson(chatModel, "system", "user");

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

            List<Float> result = AiModelSupport.embed(embeddingModel, "iPhone 14");

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

            String result = AiModelSupport.analyzeImages(
                    chatModel, List.of("http://example.com/a.jpg", "http://example.com/b.jpg"), "请描述图片内容");

            assertThat(result).isEqualTo("看到一个九成新的手机");
            verify(chatModel).call(any(Prompt.class));
        }
    }
}
