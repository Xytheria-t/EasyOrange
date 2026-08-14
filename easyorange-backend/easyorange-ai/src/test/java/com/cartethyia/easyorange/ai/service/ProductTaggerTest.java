package com.cartethyia.easyorange.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.adapter.outbound.persistence.CreditScoreFetcher;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductTagger -> 测试")
class ProductTaggerTest {

    @Mock
    private CreditScoreFetcher creditScoreFetcher;

    private ProductTagger tagger;

    @BeforeEach
    void setUp() {
        tagger = new ProductTagger(creditScoreFetcher);
    }

    private static ProductReadModel product(
            String id, String sellerId, BigDecimal price, BigDecimal originalPrice, List<String> images) {
        return new ProductReadModel(
                id,
                sellerId,
                null,
                null,
                null,
                null,
                "测试商品",
                null,
                price,
                originalPrice,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                images,
                null,
                null,
                null);
    }

    @Nested
    @DisplayName("折扣标签")
    class DiscountTagTests {

        @Test
        @DisplayName("折扣>=10% -> 超值标签")
        void discountOver10Percent() {
            when(creditScoreFetcher.fetchCreditScores(anySet())).thenReturn(Map.of());
            var p = product("1", "10", new BigDecimal("450"), new BigDecimal("500"), List.of());

            Map<String, List<String>> result = tagger.tagProducts(List.of(p));

            assertThat(result.get("1")).contains("💰超值");
        }

        @Test
        @DisplayName("折扣<10% -> 无超值标签")
        void discountUnder10Percent() {
            when(creditScoreFetcher.fetchCreditScores(anySet())).thenReturn(Map.of());
            var p = product("2", "20", new BigDecimal("460"), new BigDecimal("500"), List.of());

            Map<String, List<String>> result = tagger.tagProducts(List.of(p));

            assertThat(result.get("2")).doesNotContain("💰超值");
        }

        @Test
        @DisplayName("无原价 -> 无超值标签")
        void noOriginalPrice() {
            when(creditScoreFetcher.fetchCreditScores(anySet())).thenReturn(Map.of());
            var p = product("3", "30", new BigDecimal("300"), null, List.of());

            Map<String, List<String>> result = tagger.tagProducts(List.of(p));

            assertThat(result.get("3")).doesNotContain("💰超值");
        }
    }

    @Nested
    @DisplayName("实拍图片标签")
    class ImageTagTests {

        @Test
        @DisplayName("图片数>=3张 -> 实拍标签")
        void imagesGte3() {
            when(creditScoreFetcher.fetchCreditScores(anySet())).thenReturn(Map.of());
            var p = product("5", "50", BigDecimal.TEN, null, List.of("a.jpg", "b.jpg", "c.jpg"));

            Map<String, List<String>> result = tagger.tagProducts(List.of(p));

            assertThat(result.get("5")).contains("📸实拍");
        }

        @Test
        @DisplayName("图片数=2张 -> 无实拍标签")
        void imagesLt3() {
            when(creditScoreFetcher.fetchCreditScores(anySet())).thenReturn(Map.of());
            var p = product("6", "60", BigDecimal.TEN, null, List.of("a.jpg", "b.jpg"));

            Map<String, List<String>> result = tagger.tagProducts(List.of(p));

            assertThat(result.get("6")).doesNotContain("📸实拍");
        }

        @Test
        @DisplayName("无图片列表 -> 无实拍标签")
        void nullImages() {
            when(creditScoreFetcher.fetchCreditScores(anySet())).thenReturn(Map.of());
            var p = product("7", "70", BigDecimal.TEN, null, null);

            Map<String, List<String>> result = tagger.tagProducts(List.of(p));

            assertThat(result.get("7")).doesNotContain("📸实拍");
        }
    }

    @Nested
    @DisplayName("信用分标签")
    class CreditTagTests {

        @Test
        @DisplayName("信用分>=120 -> 信用优标签")
        void creditScoreHigh() {
            var p = product("8", "80", BigDecimal.TEN, null, List.of());
            when(creditScoreFetcher.fetchCreditScores(anySet())).thenReturn(Map.of("80", 130));

            Map<String, List<String>> result = tagger.tagProducts(List.of(p));

            assertThat(result.get("8")).contains("⭐信用优");
        }

        @Test
        @DisplayName("信用分<120 -> 无信用优标签")
        void creditScoreLow() {
            var p = product("9", "90", BigDecimal.TEN, null, List.of());
            when(creditScoreFetcher.fetchCreditScores(anySet())).thenReturn(Map.of("90", 100));

            Map<String, List<String>> result = tagger.tagProducts(List.of(p));

            assertThat(result.get("9")).doesNotContain("⭐信用优");
        }

        @Test
        @DisplayName("无信用分数据 -> 无信用优标签")
        void noCreditData() {
            var p = product("13", "130", BigDecimal.TEN, null, List.of());
            when(creditScoreFetcher.fetchCreditScores(anySet())).thenReturn(Map.of());

            Map<String, List<String>> result = tagger.tagProducts(List.of(p));

            assertThat(result.get("13")).doesNotContain("⭐信用优");
        }
    }

    @Nested
    @DisplayName("综合场景")
    class CombinedTests {

        @Test
        @DisplayName("多商品批量打标 -> 各商品独立打标")
        void multipleProducts() {
            when(creditScoreFetcher.fetchCreditScores(anySet())).thenReturn(Map.of("101", 150, "102", 90));

            var p1 = product(
                    "11", "101", new BigDecimal("400"), new BigDecimal("600"), List.of("1.jpg", "2.jpg", "3.jpg"));
            var p2 = product("12", "102", new BigDecimal("800"), new BigDecimal("800"), List.of("x.jpg"));

            Map<String, List<String>> result = tagger.tagProducts(List.of(p1, p2));

            assertThat(result.get("11")).containsExactlyInAnyOrder("💰超值", "📸实拍", "⭐信用优");
            assertThat(result.get("12")).isEmpty();
        }

        @Test
        @DisplayName("空列表 -> 返回空 map")
        void emptyList() {
            assertThat(tagger.tagProducts(List.of())).isEmpty();
        }

        @Test
        @DisplayName("null 列表 -> 返回空 map")
        void nullList() {
            assertThat(tagger.tagProducts(null)).isEmpty();
        }
    }
}
