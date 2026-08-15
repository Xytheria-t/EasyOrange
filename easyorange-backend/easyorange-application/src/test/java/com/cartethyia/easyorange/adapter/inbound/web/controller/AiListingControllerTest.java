package com.cartethyia.easyorange.adapter.inbound.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.dto.AiReviewRequest;
import com.cartethyia.easyorange.ai.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.dto.PricingRequest;
import com.cartethyia.easyorange.ai.dto.PricingSuggestion;
import com.cartethyia.easyorange.ai.service.AiPricingService;
import com.cartethyia.easyorange.ai.service.AiReviewService;
import com.cartethyia.easyorange.ai.service.AutoListingService;
import com.cartethyia.easyorange.common.result.Result;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiListingController 测试")
class AiListingControllerTest {

    @Mock
    private AiPricingService pricingService;

    @Mock
    private AutoListingService autoListingService;

    @Mock
    private AiReviewService reviewService;

    private AiListingController controller;

    @BeforeEach
    void setUp() {
        controller = new AiListingController(pricingService, autoListingService, reviewService);
    }

    @Nested
    @DisplayName("POST /api/ai/pricing")
    class SuggestPriceTests {

        @Test
        @DisplayName("请求定价 — 返回 PricingSuggestion")
        void suggestPrice_success() {
            var expected = new PricingSuggestion(
                    new BigDecimal("4500"), new BigDecimal("4200"), new BigDecimal("4800"), "成色较新，折价合理", "同款均价4500左右");
            when(pricingService.suggestPrice(anyString(), any(), any(), any(), any()))
                    .thenReturn(expected);

            var request = new PricingRequest("在管 iPhone 14", "99新", "手机数码", "2", new BigDecimal("6999"));
            Result<PricingSuggestion> result = controller.suggestPrice(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data()).isEqualTo(expected);
            assertThat(result.data().suggestedPrice()).isEqualByComparingTo(new BigDecimal("4500"));
            verify(pricingService)
                    .suggestPrice(eq("在管 iPhone 14"), eq("99新"), eq("手机数码"), eq("2"), eq(new BigDecimal("6999")));
        }

        @Test
        @DisplayName("只有商品名称时也能正常请求")
        void suggestPrice_onlyRequiredParams() {
            controller.suggestPrice(new PricingRequest("测试商品", null, null, null, null));

            verify(pricingService).suggestPrice(eq("测试商品"), isNull(), isNull(), isNull(), isNull());
        }
    }

    @Nested
    @DisplayName("POST /api/ai/auto-listing")
    class AutoListingTests {

        @Test
        @DisplayName("图片分析 — 返回 AutoListingResult")
        void autoListing_success() {
            var expected = new AutoListingResult(
                    "在管 iPhone 14",
                    "99新",
                    new BigDecimal("4500"),
                    "手机数码",
                    "1",
                    "2",
                    "广州",
                    List.of("手机", "数码"),
                    List.of("正面照片", "背面照片"));
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
            when(autoListingService.analyzeImages(anyList()))
                    .thenReturn(new AutoListingResult(null, null, null, null, null, null, null, List.of(), List.of()));

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

            var request = new AiReviewRequest(
                    "iPhone 14", "99新手机", "手机数码", "2", "¥4500", "张三", List.of("https://example.com/phone.jpg"));
            Result<AiReviewResult> result = controller.reviewProduct(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data().suggestedAction()).isTrue();
            assertThat(result.data().confidenceScore()).isEqualTo(90);
            verify(reviewService)
                    .reviewProduct(
                            eq("iPhone 14"),
                            eq("99新手机"),
                            eq("手机数码"),
                            eq("2"),
                            eq("¥4500"),
                            eq("张三"),
                            eq(List.of("https://example.com/phone.jpg")));
        }

        @Test
        @DisplayName("最少参数（只有商品名）也能正常请求")
        void reviewProduct_minimalParams() {
            when(reviewService.reviewProduct(anyString(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new AiReviewResult(true, "通过", 50, List.of(), "默认通过"));

            Result<AiReviewResult> result =
                    controller.reviewProduct(new AiReviewRequest("测试商品", null, null, null, null, null, null));

            assertThat(result.isSuccess()).isTrue();
            verify(reviewService).reviewProduct(eq("测试商品"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
        }
    }
}
