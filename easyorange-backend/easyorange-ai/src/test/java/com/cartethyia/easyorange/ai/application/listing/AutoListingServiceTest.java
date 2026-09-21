package com.cartethyia.easyorange.ai.application.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.application.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.application.support.AiModelRouter;
import com.cartethyia.easyorange.ai.domain.port.CategoryCatalogPort;
import com.cartethyia.easyorange.ai.testsupport.TestAiModelSupport;
import com.cartethyia.easyorange.ai.testsupport.TestPromptRegistry;
import com.cartethyia.easyorange.common.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoListingService 测试")
class AutoListingServiceTest {

    @Mock
    private ChatModel visionChatModel;

    @Mock
    private AiModelRouter modelRouter;

    @Mock
    private CategoryCatalogPort categoryCatalogPort;

    private AutoListingService service;

    @BeforeEach
    void setUp() {
        lenient().when(modelRouter.choose("vision")).thenReturn(visionChatModel);
        lenient().when(categoryCatalogPort.listAvailableCategoryNames()).thenReturn(List.of("手机数码", "图书教材"));
        service = new AutoListingService(
                modelRouter, new TestPromptRegistry(), TestAiModelSupport.create(), categoryCatalogPort);
    }

    private static ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private static final String VALID_JSON = """
            {"title":"二手 iPhone 14","description":"99新，无划痕","price":4500,
            "categoryName":"手机数码","conditionLevel":"2","location":"上海"}
            """;

    @Nested
    @DisplayName("analyzeImages")
    class AnalyzeImagesTests {

        @Test
        @DisplayName("一次调用产出全部字段 — 不再有第二次文本模型调用")
        void analyzeImages_singleCallProducesAllFields() {
            when(visionChatModel.call(any(Prompt.class))).thenReturn(textResponse(VALID_JSON));

            AutoListingResult result = service.analyzeImages(List.of("http://example.com/a.jpg"));

            assertThat(result.title()).isEqualTo("二手 iPhone 14");
            assertThat(result.description()).isEqualTo("99新，无划痕");
            assertThat(result.price()).isEqualByComparingTo(new BigDecimal("4500"));
            assertThat(result.categoryName()).isEqualTo("手机数码");
            assertThat(result.conditionLevel()).isEqualTo("2");
            assertThat(result.location()).isEqualTo("上海");
            // 合并调用的回归哨兵：视觉模型只被调用一次
            verify(visionChatModel).call(any(Prompt.class));
        }

        @Test
        @DisplayName("可用类目清单随 user 消息交给模型（约束它从枚举里挑，不自造类目名）")
        void analyzeImages_injectsCategoryCatalog() {
            when(visionChatModel.call(any(Prompt.class))).thenReturn(textResponse(VALID_JSON));

            service.analyzeImages(List.of("http://example.com/a.jpg"));

            var captor = ArgumentCaptor.forClass(Prompt.class);
            verify(visionChatModel).call(captor.capture());
            String userText = captor.getValue().getInstructions().stream()
                    .map(Message::getText)
                    .collect(Collectors.joining("\n"));
            assertThat(userText).contains("手机数码", "图书教材");
        }

        @Test
        @DisplayName("模型返回空内容 — 抛 B8002（用户看得见，而不是 HTTP 200 + null）")
        void analyzeImages_emptyModelOutput() {
            when(visionChatModel.call(any(Prompt.class))).thenReturn(textResponse(null));

            assertThatThrownBy(() -> service.analyzeImages(List.of("http://example.com/a.jpg")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo("B8002");
        }

        @Test
        @DisplayName("模型返回非法 JSON — 抛 B8002")
        void analyzeImages_invalidJson() {
            when(visionChatModel.call(any(Prompt.class))).thenReturn(textResponse("{broken}"));

            assertThatThrownBy(() -> service.analyzeImages(List.of("http://example.com/a.jpg")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo("B8002");
        }

        @Test
        @DisplayName("供应商抛异常（模型未配置 / 超时）— 抛 B8002")
        void analyzeImages_llmThrows() {
            when(visionChatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("vision timeout"));

            assertThatThrownBy(() -> service.analyzeImages(List.of("http://example.com/a.jpg")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo("B8002");
        }

        @Test
        @DisplayName("Prompt 模板缺失时抛 IllegalStateException（配置错误 fail-fast，不伪装成 AI 不可用）")
        void analyzeImages_missingPrompt() {
            service = new AutoListingService(
                    modelRouter, TestPromptRegistry.empty(), TestAiModelSupport.create(), categoryCatalogPort);

            assertThatThrownBy(() -> service.analyzeImages(List.of("http://example.com/a.jpg")))
                    .isInstanceOf(IllegalStateException.class);
            verify(visionChatModel, never()).call(any(Prompt.class));
        }
    }
}
