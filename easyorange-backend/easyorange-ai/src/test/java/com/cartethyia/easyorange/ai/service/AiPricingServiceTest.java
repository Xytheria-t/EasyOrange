package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.dto.PricingSuggestion;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiPricingService 测试")
@SuppressWarnings("unchecked")
class AiPricingServiceTest {

    @Mock
    private LlmPort llmPort;

    @Mock
    private ObjectMapper objectMapper;

    private AiPricingService service;

    @BeforeEach
    void setUp() {
        service = new AiPricingService(llmPort, objectMapper);
    }

    @Nested
    @DisplayName("suggestPrice")
    class SuggestPriceTests {

        @Test
        @DisplayName("正常定价逻辑 — 输入商品信息返回定价建议")
        void suggestPrice_success() throws Exception {
            String productName = "在管 iPhone 14";
            String description = "99新，使用3个月，配件齐全";
            String categoryName = "手机数码";
            String conditionLevel = "2";
            BigDecimal originalPrice = new BigDecimal("6999");

            String jsonResponse = """
                    {"suggestedPrice":4500,"minPrice":4200,"maxPrice":4800,
                    "reasoning":"成色较新，折价合理","marketContext":"同款均价4500左右"}
                    """;
            PricingSuggestion expected = new PricingSuggestion(
                    new BigDecimal("4500"),
                    new BigDecimal("4200"),
                    new BigDecimal("4800"),
                    "成色较新，折价合理",
                    "同款均价4500左右"
            );

            when(llmPort.generateTextWithJson(anyString(), anyString())).thenReturn(jsonResponse);
            when(objectMapper.readValue(jsonResponse, PricingSuggestion.class)).thenReturn(expected);

            PricingSuggestion result = service.suggestPrice(
                    productName, description, categoryName, conditionLevel, originalPrice
            );

            assertThat(result).isNotNull();
            assertThat(result.suggestedPrice()).isEqualByComparingTo(new BigDecimal("4500"));
            assertThat(result.minPrice()).isEqualByComparingTo(new BigDecimal("4200"));
            assertThat(result.maxPrice()).isEqualByComparingTo(new BigDecimal("4800"));
            assertThat(result.reasoning()).isEqualTo("成色较新，折价合理");
            assertThat(result.marketContext()).isEqualTo("同款均价4500左右");
            verify(llmPort).generateTextWithJson(anyString(), anyString());
            verify(objectMapper).readValue(jsonResponse, PricingSuggestion.class);
        }

        @Test
        @DisplayName("LLM 返回空时返回 null")
        void suggestPrice_llmReturnsNull() throws Exception {
            when(llmPort.generateTextWithJson(anyString(), anyString())).thenReturn(null);

            PricingSuggestion result = service.suggestPrice("测试商品", null, null, null, null);

            assertThat(result).isNull();
            verify(llmPort).generateTextWithJson(anyString(), anyString());
            verify(objectMapper, never()).readValue(anyString(), any(Class.class));
        }

        @Test
        @DisplayName("JSON 解析异常时返回 null")
        void suggestPrice_jsonParseException() throws Exception {
            String invalidJson = "{not valid json}";

            when(llmPort.generateTextWithJson(anyString(), anyString())).thenReturn(invalidJson);
            when(objectMapper.readValue(invalidJson, PricingSuggestion.class))
                    .thenThrow(JacksonException.class);

            PricingSuggestion result = service.suggestPrice(
                    "测试商品", "描述", "分类", "1", new BigDecimal("100")
            );

            assertThat(result).isNull();
            verify(llmPort).generateTextWithJson(anyString(), anyString());
        }

        @Test
        @DisplayName("LLM 抛出运行时异常时返回 null")
        void suggestPrice_llmThrowsException() {
            when(llmPort.generateTextWithJson(anyString(), anyString()))
                    .thenThrow(new RuntimeException("API connection timeout"));

            PricingSuggestion result = service.suggestPrice(
                    "测试商品", "描述", "分类", "1", new BigDecimal("100")
            );

            assertThat(result).isNull();
        }
    }
}
