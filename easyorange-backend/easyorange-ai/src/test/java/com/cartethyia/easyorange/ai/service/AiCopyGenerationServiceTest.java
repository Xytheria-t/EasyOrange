package com.cartethyia.easyorange.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.adapter.outbound.AiCallLogRecorder;
import com.cartethyia.easyorange.ai.service.AiModelSupport;
import com.cartethyia.easyorange.ai.dto.CopyGenerationResult;
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
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiCopyGenerationService 测试")
class AiCopyGenerationServiceTest {

    @Mock
    private ChatModel chatModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AiCopyGenerationService service;

    @BeforeEach
    void setUp() {
        service = new AiCopyGenerationService(chatModel, objectMapper, new TestPromptRegistry(), new AiModelSupport(mock(AiCallLogRecorder.class)));
    }

    private static ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private static final String VALID_JSON = """
            {"title":"苹果 iPhone 14 128G","description":"99新，配件齐全","style":"standard"}
            """;

    @Nested
    @DisplayName("generateCopy")
    class GenerateCopyTests {

        @Test
        @DisplayName("正常生成 — 返回营销文案")
        void generateCopy_success() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse(VALID_JSON));

            CopyGenerationResult result = service.generateCopy("iPhone 14", "手机数码", "2", "6999", "standard");

            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("苹果 iPhone 14 128G");
            assertThat(result.description()).isEqualTo("99新，配件齐全");
            assertThat(result.style()).isEqualTo("standard");
            verify(chatModel).call(any(Prompt.class));
        }

        @Test
        @DisplayName("different style / condition / null 输入仍可生成")
        void generateCopy_styleVariants() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse(VALID_JSON));

            CopyGenerationResult detailed = service.generateCopy("A", "B", "1", "100", "detailed");
            CopyGenerationResult concise = service.generateCopy("A", null, "3", "200", "concise");
            CopyGenerationResult emotional = service.generateCopy("A", "B", "4", null, "emotional");
            CopyGenerationResult unknown = service.generateCopy("A", "B", "9", "", null);

            assertThat(detailed).isNotNull();
            assertThat(concise).isNotNull();
            assertThat(emotional).isNotNull();
            assertThat(unknown).isNotNull();
            verify(chatModel, times(4)).call(any(Prompt.class));
        }

        @Test
        @DisplayName("LLM 返回 null 时返回 null")
        void generateCopy_llmReturnsNull() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse(null));

            CopyGenerationResult result = service.generateCopy("A", "B", "1", "100", null);

            assertThat(result).isNull();
            verify(chatModel).call(any(Prompt.class));
        }

        @Test
        @DisplayName("LLM 返回非法 JSON 时返回 null")
        void generateCopy_invalidJson() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("{not valid}"));

            CopyGenerationResult result = service.generateCopy("A", "B", "1", "100", null);

            assertThat(result).isNull();
            verify(chatModel).call(any(Prompt.class));
        }

        @Test
        @DisplayName("LLM 抛出异常时返回 null")
        void generateCopy_llmThrows() {
            when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API timeout"));

            CopyGenerationResult result = service.generateCopy("A", "B", "1", "100", null);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Prompt 模板缺失时抛 IllegalStateException")
        void generateCopy_missingPrompt() {
            service = new AiCopyGenerationService(chatModel, objectMapper, EMPTY_REGISTRY, new AiModelSupport(mock(AiCallLogRecorder.class)));

            assertThatThrownBy(() -> service.generateCopy("A", "B", "1", "100", null))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    private static final com.cartethyia.easyorange.ai.prompt.PromptRegistry EMPTY_REGISTRY =
            new com.cartethyia.easyorange.ai.prompt.PromptRegistry() {
                @Override
                public java.util.Optional<com.cartethyia.easyorange.ai.prompt.PromptTemplate> get(
                        String name, String version) {
                    return java.util.Optional.empty();
                }

                @Override
                public java.util.Optional<com.cartethyia.easyorange.ai.prompt.PromptTemplate> getLatest(String name) {
                    return java.util.Optional.empty();
                }

                @Override
                public java.util.List<com.cartethyia.easyorange.ai.prompt.PromptTemplate> listVersions(String name) {
                    return java.util.List.of();
                }
            };
}
