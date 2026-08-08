package com.cartethyia.easyorange.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.adapter.outbound.AiCallLogRecorder;
import com.cartethyia.easyorange.ai.service.AiModelSupport;
import com.cartethyia.easyorange.ai.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.prompt.PromptRegistry;
import com.cartethyia.easyorange.ai.prompt.PromptTemplate;
import com.cartethyia.easyorange.ai.prompt.TestPromptRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
@DisplayName("AutoListingService 测试")
class AutoListingServiceTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private ChatModel visionChatModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AutoListingService service;

    @BeforeEach
    void setUp() {
        service = new AutoListingService(chatModel, visionChatModel, objectMapper, new TestPromptRegistry(), new AiModelSupport(mock(AiCallLogRecorder.class)));
    }

    private static ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private static final String VALID_JSON = """
            {"title":"二手 iPhone 14","description":"99新","price":4500,
            "categoryName":"手机数码","categoryId":"c1","conditionLevel":"2",
            "location":"上海","tags":["手机","苹果"],"imageDescriptions":["正面照","背面照"]}
            """;

    @Nested
    @DisplayName("analyzeImages")
    class AnalyzeImagesTests {

        @Test
        @DisplayName("正常流程 — 视觉识别 + 结构化生成")
        void analyzeImages_success() {
            when(visionChatModel.call(any(Prompt.class))).thenReturn(textResponse("图片中是一部九成新的 iPhone 14"));
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse(VALID_JSON));

            AutoListingResult result = service.analyzeImages(List.of("http://example.com/a.jpg"));

            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("二手 iPhone 14");
            assertThat(result.price()).isEqualByComparingTo(new BigDecimal("4500"));
            assertThat(result.tags()).containsExactly("手机", "苹果");
            verify(visionChatModel).call(any(Prompt.class));
            verify(chatModel).call(any(Prompt.class));
        }

        @Test
        @DisplayName("视觉模型返回 null 时返回 null")
        void analyzeImages_visionReturnsNull() {
            when(visionChatModel.call(any(Prompt.class))).thenReturn(textResponse(null));

            AutoListingResult result = service.analyzeImages(List.of("http://example.com/a.jpg"));

            assertThat(result).isNull();
            verify(chatModel, never()).call(any(Prompt.class));
        }

        @Test
        @DisplayName("文本模型返回 null 时返回 null")
        void analyzeImages_textReturnsNull() {
            when(visionChatModel.call(any(Prompt.class))).thenReturn(textResponse("vision ok"));
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse(null));

            AutoListingResult result = service.analyzeImages(List.of("http://example.com/a.jpg"));

            assertThat(result).isNull();
            verify(chatModel).call(any(Prompt.class));
        }

        @Test
        @DisplayName("文本模型返回非法 JSON 时返回 null")
        void analyzeImages_invalidJson() {
            when(visionChatModel.call(any(Prompt.class))).thenReturn(textResponse("vision ok"));
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("{broken}"));

            AutoListingResult result = service.analyzeImages(List.of("http://example.com/a.jpg"));

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("LLM 抛出异常时返回 null")
        void analyzeImages_llmThrows() {
            when(visionChatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("vision timeout"));

            AutoListingResult result = service.analyzeImages(List.of("http://example.com/a.jpg"));

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Prompt 模板缺失时返回 null（被 catch 兜底）")
        void analyzeImages_missingPrompt() {
            service = new AutoListingService(chatModel, visionChatModel, objectMapper, EMPTY_REGISTRY, new AiModelSupport(mock(AiCallLogRecorder.class)));

            AutoListingResult result = service.analyzeImages(List.of("http://example.com/a.jpg"));

            assertThat(result).isNull();
            verify(visionChatModel, never()).call(any(Prompt.class));
        }
    }

    private static final PromptRegistry EMPTY_REGISTRY = new PromptRegistry() {
        @Override
        public Optional<PromptTemplate> get(String name, String version) {
            return Optional.empty();
        }

        @Override
        public Optional<PromptTemplate> getLatest(String name) {
            return Optional.empty();
        }

        @Override
        public List<PromptTemplate> listVersions(String name) {
            return List.of();
        }
    };
}
