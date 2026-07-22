package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.dto.AiReviewRequest;
import com.cartethyia.easyorange.ai.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.dto.CopyGenerationRequest;
import com.cartethyia.easyorange.ai.dto.CopyGenerationResult;
import com.cartethyia.easyorange.ai.dto.PricingRequest;
import com.cartethyia.easyorange.ai.dto.PricingSuggestion;
import com.cartethyia.easyorange.ai.dto.QaRequest;
import com.cartethyia.easyorange.ai.dto.QaResponse;
import com.cartethyia.easyorange.ai.dto.SemanticSearchResult;
import com.cartethyia.easyorange.ai.service.AiCopyGenerationService;
import com.cartethyia.easyorange.ai.service.AiPricingService;
import com.cartethyia.easyorange.ai.service.AiQaService;
import com.cartethyia.easyorange.ai.service.AiReviewService;
import com.cartethyia.easyorange.ai.service.AutoListingService;
import com.cartethyia.easyorange.ai.service.SemanticSearchService;
import com.cartethyia.easyorange.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiController 测试")
class AiControllerTest {

    @Mock
    private AiPricingService pricingService;

    @Mock
    private AutoListingService autoListingService;

    @Mock
    private AiReviewService reviewService;

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private AiQaService qaService;

    @Mock
    private AiCopyGenerationService copyGenerationService;

    private AiController controller;

    @BeforeEach
    void setUp() {
        controller = new AiController(
                pricingService, autoListingService, reviewService,
                semanticSearchService, qaService, copyGenerationService
        );
    }

    @Nested
    @DisplayName("POST /api/ai/pricing")
    class SuggestPriceTests {

        @Test
        @DisplayName("请求定价 — 返回 PricingSuggestion")
        void suggestPrice_success() {
            var expected = new PricingSuggestion(
                    new BigDecimal("4500"), new BigDecimal("4200"),
                    new BigDecimal("4800"), "成色较新，折价合理", "同款均价4500左右"
            );
            when(pricingService.suggestPrice(anyString(), any(), any(), any(), any()))
                    .thenReturn(expected);

            var request = new PricingRequest("在管 iPhone 14", "99新", "手机数码", "2", new BigDecimal("6999"));
            Result<PricingSuggestion> result = controller.suggestPrice(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data()).isEqualTo(expected);
            assertThat(result.data().suggestedPrice()).isEqualByComparingTo(new BigDecimal("4500"));
            verify(pricingService).suggestPrice(eq("在管 iPhone 14"), eq("99新"),
                    eq("手机数码"), eq("2"), eq(new BigDecimal("6999")));
        }

        @Test
        @DisplayName("只有商品名称时也能正常请求")
        void suggestPrice_onlyRequiredParams() {
            controller.suggestPrice(new PricingRequest("测试商品", null, null, null, null));

            verify(pricingService).suggestPrice(eq("测试商品"), isNull(),
                    isNull(), isNull(), isNull());
        }
    }

    @Nested
    @DisplayName("POST /api/ai/auto-listing")
    class AutoListingTests {

        @Test
        @DisplayName("图片分析 — 返回 AutoListingResult")
        void autoListing_success() {
            var expected = new AutoListingResult(
                    "在管 iPhone 14", "99新", new BigDecimal("4500"),
                    "手机数码", 1, "2", "广州",
                    List.of("手机", "数码"), List.of("正面照片", "背面照片")
            );
            when(autoListingService.analyzeImages(anyList())).thenReturn(expected);

            var imageUrls = List.of("https://example.com/img1.jpg", "https://example.com/img2.jpg");
            Result<AutoListingResult> result = controller.autoListing(imageUrls);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data()).isEqualTo(expected);
            assertThat(result.data().title()).isEqualTo("在管 iPhone 14");
            verify(autoListingService).analyzeImages(imageUrls);
        }

        @Test
        @DisplayName("空图片列表 — 仍可正常请求")
        void autoListing_emptyImages() {
            when(autoListingService.analyzeImages(anyList())).thenReturn(
                    new AutoListingResult(null, null, null, null, null, null, null, List.of(), List.of())
            );

            Result<AutoListingResult> result = controller.autoListing(List.of());

            assertThat(result.isSuccess()).isTrue();
            verify(autoListingService).analyzeImages(List.of());
        }
    }

    @Nested
    @DisplayName("POST /api/ai/review")
    class ReviewProductTests {

        @Test
        @DisplayName("审核商品 — 返回 AiReviewResult")
        void reviewProduct_success() {
            var expected = new AiReviewResult(true, "通过", 90, List.of(), "信息完整合规");
            when(reviewService.reviewProduct(anyString(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(expected);

            var request = new AiReviewRequest("iPhone 14", "99新手机", "手机数码", "2", "¥4500", "张三",
                    List.of("https://example.com/phone.jpg"));
            Result<AiReviewResult> result = controller.reviewProduct(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data().suggestedAction()).isTrue();
            assertThat(result.data().confidenceScore()).isEqualTo(90);
            verify(reviewService).reviewProduct(eq("iPhone 14"), eq("99新手机"),
                    eq("手机数码"), eq("2"), eq("¥4500"), eq("张三"),
                    eq(List.of("https://example.com/phone.jpg")));
        }

        @Test
        @DisplayName("最少参数（只有商品名）也能正常请求")
        void reviewProduct_minimalParams() {
            when(reviewService.reviewProduct(anyString(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new AiReviewResult(true, "通过", 50, List.of(), "默认通过"));

            Result<AiReviewResult> result = controller.reviewProduct(new AiReviewRequest("测试商品", null, null, null, null, null, null));

            assertThat(result.isSuccess()).isTrue();
            verify(reviewService).reviewProduct(eq("测试商品"), isNull(),
                    isNull(), isNull(), isNull(), isNull(), isNull());
        }
    }

    @Nested
    @DisplayName("GET /api/ai/semantic-search")
    class SemanticSearchTests {

        @Test
        @DisplayName("语义搜索 — 返回搜索结果")
        void semanticSearch_success() {
            var expected = new SemanticSearchResult(List.of(), 0L, 1, 20);
            when(semanticSearchService.search(anyString(), anyInt(), anyInt())).thenReturn(expected);

            Result<SemanticSearchResult> result = controller.semanticSearch("iPhone", 1, 20);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data().total()).isZero();
            assertThat(result.data().current()).isEqualTo(1);
            assertThat(result.data().size()).isEqualTo(20);
            verify(semanticSearchService).search("iPhone", 1, 20);
        }

        @Test
        @DisplayName("使用默认分页参数")
        void semanticSearch_defaultPagination() {
            var expected = new SemanticSearchResult(List.of(), 0L, 1, 20);
            when(semanticSearchService.search(anyString(), anyInt(), anyInt())).thenReturn(expected);

            Result<SemanticSearchResult> result = controller.semanticSearch("手机", 1, 20);

            assertThat(result.isSuccess()).isTrue();
            verify(semanticSearchService).search("手机", 1, 20);
        }
    }

    @Nested
    @DisplayName("POST /api/ai/qa")
    class AnswerQuestionTests {

        @Test
        @DisplayName("问答 — 返回有效回答")
        void answerQuestion_success() {
            var request = new QaRequest(1L, "是正品吗？", "iPhone 14", "99新",
                    "手机数码", "¥4500", "九五新", "张三", "高");
            var expected = new QaResponse("是正品，有官方购买凭证", true);
            when(qaService.answerQuestion(any())).thenReturn(expected);

            Result<QaResponse> result = controller.answerQuestion(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data().answer()).isEqualTo("是正品，有官方购买凭证");
            assertThat(result.data().confidence()).isTrue();
            verify(qaService).answerQuestion(request);
        }
    }

    @Nested
    @DisplayName("POST /api/ai/generate-copy")
    class GenerateCopyTests {

        @Test
        @DisplayName("文案生成 — 返回 CopyGenerationResult")
        void generateCopy_success() {
            var expected = new CopyGenerationResult("超值iPhone 14", "详细描述...", "standard");
            when(copyGenerationService.generateCopy(anyString(), any(), any(), any(), anyString()))
                    .thenReturn(expected);

            var request = new CopyGenerationRequest("iPhone 14", "手机数码", "2", "¥6999", "standard");
            Result<CopyGenerationResult> result = controller.generateCopy(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data().title()).isEqualTo("超值iPhone 14");
            assertThat(result.data().style()).isEqualTo("standard");
            verify(copyGenerationService).generateCopy(eq("iPhone 14"), eq("手机数码"),
                    eq("2"), eq("¥6999"), eq("standard"));
        }

        @Test
        @DisplayName("使用默认风格 standard")
        void generateCopy_defaultStyle() {
            when(copyGenerationService.generateCopy(anyString(), any(), any(), any(), eq("standard")))
                    .thenReturn(new CopyGenerationResult("标题", "描述", "standard"));

            var request = new CopyGenerationRequest("测试商品", null, null, null, "standard");
            Result<CopyGenerationResult> result = controller.generateCopy(request);

            assertThat(result.isSuccess()).isTrue();
            verify(copyGenerationService).generateCopy(eq("测试商品"), isNull(),
                    isNull(), isNull(), eq("standard"));
        }

        @Test
        @DisplayName("不同风格参数 — 传递正确")
        void generateCopy_differentStyles() {
            when(copyGenerationService.generateCopy(anyString(), any(), any(), any(), anyString()))
                    .thenReturn(new CopyGenerationResult("标题", "描述", "detailed"));

            var request = new CopyGenerationRequest("商品", "分类", "1", "¥100", "detailed");
            Result<CopyGenerationResult> result = controller.generateCopy(request);

            assertThat(result.isSuccess()).isTrue();
            verify(copyGenerationService).generateCopy(eq("商品"), eq("分类"),
                    eq("1"), eq("¥100"), eq("detailed"));
        }
    }
}
