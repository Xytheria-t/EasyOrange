package com.cartethyia.easyorange.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.application.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.testsupport.TestAiModelSupport;
import com.cartethyia.easyorange.ai.testsupport.TestPromptRegistry;
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
@DisplayName("AiReviewService 测试")
class AiReviewServiceTest {

    @Mock
    private ChatModel chatModel;

    private AiReviewService service;

    @BeforeEach
    void setUp() {
        service = new AiReviewService(chatModel, new TestPromptRegistry(), TestAiModelSupport.create());
    }

    private static ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Nested
    @DisplayName("reviewProduct")
    class ReviewProductTests {

        @Test
        @DisplayName("审核通过 — 信息完整合规")
        void reviewProduct_approved() {
            String jsonResponse = """
                    {"suggestedAction":true,"suggestedActionDesc":"通过",
                    "confidenceScore":90,"riskFlags":[],"reasoning":"信息完整合规"}
                    """;

            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse(jsonResponse));

            AiReviewResult result = service.reviewProduct(
                    "iPhone 14", "99新手机", "手机数码", "2", "¥4500", "张三", List.of("https://example.com/phone.jpg"));

            assertThat(result.suggestedAction()).isTrue();
            assertThat(result.suggestedActionDesc()).isEqualTo("通过");
            assertThat(result.confidenceScore()).isEqualTo(90);
            assertThat(result.riskFlags()).isEmpty();
            assertThat(result.reasoning()).isEqualTo("信息完整合规");
        }

        @Test
        @DisplayName("审核拒绝 — 价格异常")
        void reviewProduct_rejected() {
            String jsonResponse = """
                    {"suggestedAction":false,"suggestedActionDesc":"拒绝",
                    "confidenceScore":85,"riskFlags":["价格异常","描述不符"],
                    "reasoning":"价格明显异常"}
                    """;

            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse(jsonResponse));

            AiReviewResult result =
                    service.reviewProduct("Gucci 包", "正品", "奢侈品", "1", "¥999999", "资产方", List.of("url1", "url2"));

            assertThat(result.suggestedAction()).isFalse();
            assertThat(result.suggestedActionDesc()).isEqualTo("拒绝");
            assertThat(result.riskFlags()).contains("价格异常", "描述不符");
        }

        @Test
        @DisplayName("LLM 返回空 -> 降级为「无法判定」，不给「通过」（isApproved 驱动一键通过按钮）")
        void reviewProduct_llmReturnsNull() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse(null));

            AiReviewResult result = service.reviewProduct("测试商品", "描述", "分类", "1", "¥100", "资产方", List.of());

            assertUnavailable(result);
        }

        @Test
        @DisplayName("LLM 调用异常 -> 降级为「无法判定」（AI 挂掉不能被读成平台放行）")
        void reviewProduct_llmException() {
            when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API error"));

            AiReviewResult result = service.reviewProduct("测试商品", "描述", "分类", "1", "¥100", "资产方", null);

            assertUnavailable(result);
        }

        @Test
        @DisplayName("JSON 解析异常 -> 降级为「无法判定」")
        void reviewProduct_jsonParseException() {
            when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("{invalid}"));

            AiReviewResult result = service.reviewProduct("测试商品", "描述", "分类", "1", "¥100", "资产方", List.of("url"));

            assertUnavailable(result);
        }

        @Test
        @DisplayName("prompt 模板缺失 -> 抛 IllegalStateException（配置错误 fail-fast，不伪装成 AI 不可用）")
        void reviewProduct_missingPrompt() {
            service = new AiReviewService(chatModel, TestPromptRegistry.empty(), TestAiModelSupport.create());

            assertThatThrownBy(() -> service.reviewProduct("商品", "描述", "分类", "1", "¥100", "资产方", List.of()))
                    .isInstanceOf(IllegalStateException.class);
            verify(chatModel, never()).call(any(Prompt.class));
        }

        /**
         * 模型故障 / 返回空 / JSON 不合 schema 三种情况都收敛到同一降级结果：
         * 差异只在 {@code AiModelSupport} 的 warn 日志里，对管理端而言都是「这条建议无效」。
         */
        private static void assertUnavailable(AiReviewResult result) {
            assertThat(result.suggestedAction())
                    .as("降级方向必须是「不通过」：true 会让管理端渲染出「采纳 AI 建议=通过」的按钮")
                    .isFalse();
            assertThat(result.suggestedActionDesc()).isEqualTo("无法判定");
            assertThat(result.confidenceScore()).isZero();
            assertThat(result.riskFlags()).containsExactly(AiReviewService.FLAG_UNAVAILABLE);
            assertThat(result.reasoning()).isEqualTo("AI 无法分析，请人工审核");
        }
    }
}
