package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.port.LlmPort;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiReviewService 测试")
class AiReviewServiceTest {

    @Mock
    private LlmPort llmPort;

    @Mock
    private ObjectMapper objectMapper;

    private AiReviewService service;

    @BeforeEach
    void setUp() {
        service = new AiReviewService(llmPort, objectMapper);
    }

    @Nested
    @DisplayName("reviewProduct")
    class ReviewProductTests {

        @Test
        @DisplayName("审核通过 — 信息完整合规")
        void reviewProduct_approved() throws Exception {
            String jsonResponse = """
                    {"suggestedAction":true,"suggestedActionDesc":"通过",
                    "confidenceScore":90,"riskFlags":[],"reasoning":"信息完整合规"}
                    """;
            AiReviewResult expected = new AiReviewResult(true, "通过", 90, List.of(), "信息完整合规");

            when(llmPort.generateTextWithJson(anyString(), anyString())).thenReturn(jsonResponse);
            when(objectMapper.readValue(jsonResponse, AiReviewResult.class)).thenReturn(expected);

            AiReviewResult result = service.reviewProduct(
                    "iPhone 14", "99新手机", "手机数码", "2",
                    "¥4500", "张三", List.of("https://example.com/phone.jpg")
            );

            assertThat(result.suggestedAction()).isTrue();
            assertThat(result.suggestedActionDesc()).isEqualTo("通过");
            assertThat(result.confidenceScore()).isEqualTo(90);
            assertThat(result.riskFlags()).isEmpty();
            assertThat(result.reasoning()).isEqualTo("信息完整合规");
        }

        @Test
        @DisplayName("审核拒绝 — 价格异常")
        void reviewProduct_rejected() throws Exception {
            String jsonResponse = """
                    {"suggestedAction":false,"suggestedActionDesc":"拒绝",
                    "confidenceScore":85,"riskFlags":["价格异常","描述不符"],
                    "reasoning":"价格明显异常"}
                    """;
            AiReviewResult expected = new AiReviewResult(
                    false, "拒绝", 85, List.of("价格异常", "描述不符"), "价格明显异常"
            );

            when(llmPort.generateTextWithJson(anyString(), anyString())).thenReturn(jsonResponse);
            when(objectMapper.readValue(jsonResponse, AiReviewResult.class)).thenReturn(expected);

            AiReviewResult result = service.reviewProduct(
                    "Gucci 包", "正品", "奢侈品", "1",
                    "¥999999", "资产方", List.of("url1", "url2")
            );

            assertThat(result.suggestedAction()).isFalse();
            assertThat(result.suggestedActionDesc()).isEqualTo("拒绝");
            assertThat(result.riskFlags()).contains("价格异常", "描述不符");
        }

        @Test
        @DisplayName("LLM 返回空时默认通过")
        void reviewProduct_llmReturnsNull() {
            when(llmPort.generateTextWithJson(anyString(), anyString())).thenReturn(null);

            AiReviewResult result = service.reviewProduct(
                    "测试商品", "描述", "分类", "1", "¥100", "资产方", List.of()
            );

            assertThat(result.suggestedAction()).isTrue();
            assertThat(result.suggestedActionDesc()).isEqualTo("通过");
            assertThat(result.confidenceScore()).isEqualTo(50);
            assertThat(result.reasoning()).isEqualTo("AI 无法分析，默认通过");
        }

        @Test
        @DisplayName("LLM 调用异常时返回默认通过")
        void reviewProduct_llmException() {
            when(llmPort.generateTextWithJson(anyString(), anyString()))
                    .thenThrow(new RuntimeException("API error"));

            AiReviewResult result = service.reviewProduct(
                    "测试商品", "描述", "分类", "1", "¥100", "资产方", null
            );

            assertThat(result.suggestedAction()).isTrue();
            assertThat(result.suggestedActionDesc()).isEqualTo("通过");
            assertThat(result.confidenceScore()).isEqualTo(50);
            assertThat(result.reasoning()).isEqualTo("AI 分析异常，默认通过");
        }

        @Test
        @DisplayName("JSON 解析异常时返回默认通过")
        void reviewProduct_jsonParseException() throws Exception {
            String invalidJson = "{invalid}";

            when(llmPort.generateTextWithJson(anyString(), anyString())).thenReturn(invalidJson);
            when(objectMapper.readValue(invalidJson, AiReviewResult.class))
                    .thenThrow(JacksonException.class);

            AiReviewResult result = service.reviewProduct(
                    "测试商品", "描述", "分类", "1", "¥100", "资产方", List.of("url")
            );

            assertThat(result.suggestedAction()).isTrue();
            assertThat(result.suggestedActionDesc()).isEqualTo("通过");
            assertThat(result.confidenceScore()).isEqualTo(50);
        }
    }
}
