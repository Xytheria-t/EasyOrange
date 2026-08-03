package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.port.query.ProductRatingQueryRepository;
import com.cartethyia.easyorange.product.application.query.dto.ProductRatingVO;
import com.cartethyia.easyorange.product.application.query.dto.RatingStatsVO;
import com.cartethyia.easyorange.product.domain.entity.ProductRating;
import com.cartethyia.easyorange.product.domain.port.SellerInfoPort;
import com.cartethyia.easyorange.product.domain.valueobject.SellerInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductRatingQueryHandler 测试")
class ProductRatingQueryHandlerTest {

    @Mock
    private ProductRatingQueryRepository productRatingQueryRepository;

    @Mock
    private SellerInfoPort sellerInfoPort;

    private ProductRatingQueryHandler queryHandler;

    private ProductRating review;

    @BeforeEach
    void setUp() {
        queryHandler = new ProductRatingQueryHandler(productRatingQueryRepository, sellerInfoPort);

        review = ProductRating.reconstitute("100", "10", "1", null,
                5, "非常好", null, null,
                3, 1, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("查询评价列表应返回分页结果")
    void listReviews_shouldReturnPage() {
        PageResult<ProductRating> page = PageResult.of(List.of(review), 1, 1, 10);
        when(productRatingQueryRepository.findByProductId("10", 1, 10)).thenReturn(page);
        when(sellerInfoPort.getSellerInfos(anySet())).thenReturn(Map.of(
                "1", new SellerInfo("1", "认领方", null, "http://avatar.jpg")
        ));

        PageResult<ProductRatingVO> result = queryHandler.listReviews("10", 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.records()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
        ProductRatingVO vo = result.records().get(0);
        assertThat(vo.getId()).isEqualTo("100");
        assertThat(vo.getProductId()).isEqualTo("10");
        assertThat(vo.getRating()).isEqualTo(5);
        assertThat(vo.getContent()).isEqualTo("非常好");
        assertThat(vo.getLikes()).isEqualTo(3);
        assertThat(vo.getUsername()).isEqualTo("认领方");
        assertThat(vo.getUserAvatar()).isEqualTo("http://avatar.jpg");
    }

    @Test
    @DisplayName("评价列表为空时应返回空分页")
    void listReviews_withEmptyResult_shouldReturnEmptyPage() {
        PageResult<ProductRating> page = PageResult.of(List.of(), 0L, 1, 10);
        when(productRatingQueryRepository.findByProductId("10", 1, 10)).thenReturn(page);

        PageResult<ProductRatingVO> result = queryHandler.listReviews("10", 1, 10);

        assertThat(result.records()).isEmpty();
        assertThat(result.total()).isZero();
        verify(sellerInfoPort, never()).getSellerInfos(anySet());
    }

    @Test
    @DisplayName("用户不存在时应使用默认用户名")
    void listReviews_whenUserNotFound_shouldUseDefault() {
        PageResult<ProductRating> page = PageResult.of(List.of(review), 1, 1, 10);
        when(productRatingQueryRepository.findByProductId("10", 1, 10)).thenReturn(page);
        when(sellerInfoPort.getSellerInfos(anySet())).thenReturn(Map.of());

        PageResult<ProductRatingVO> result = queryHandler.listReviews("10", 1, 10);

        assertThat(result.records().get(0).getUsername()).isEqualTo("未知用户");
        assertThat(result.records().get(0).getUserAvatar()).isNull();
    }

    @Test
    @DisplayName("获取评价统计信息应正确计算")
    void getReviewStats_shouldCalculateCorrectly() {
        ProductRating review2 = ProductRating.reconstitute("101", "10", "2", null,
                4, "好", null, null,
                1, 1, LocalDateTime.now(), LocalDateTime.now());
        ProductRating review3 = ProductRating.reconstitute("102", "10", "3", null,
                5, "很好", null, null,
                2, 1, LocalDateTime.now(), LocalDateTime.now());

        when(productRatingQueryRepository.findAllByProductId("10"))
                .thenReturn(List.of(review, review2, review3));

        RatingStatsVO stats = queryHandler.getReviewStats("10");

        assertThat(stats.getProductId()).isEqualTo("10");
        assertThat(stats.getTotalCount()).isEqualTo(3);
        assertThat(stats.getAverageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.7));
        assertThat(stats.getRatingDistribution())
                .containsEntry(4, 1L)
                .containsEntry(5, 2L)
                .containsEntry(1, 0L)
                .containsEntry(2, 0L)
                .containsEntry(3, 0L);
    }

    @Test
    @DisplayName("没有评价时应返回零值统计")
    void getReviewStats_withNoReviews_shouldReturnZeroStats() {
        when(productRatingQueryRepository.findAllByProductId("10")).thenReturn(List.of());

        RatingStatsVO stats = queryHandler.getReviewStats("10");

        assertThat(stats.getProductId()).isEqualTo("10");
        assertThat(stats.getTotalCount()).isZero();
        assertThat(stats.getAverageRating()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getRatingDistribution())
                .containsEntry(1, 0L)
                .containsEntry(2, 0L)
                .containsEntry(3, 0L)
                .containsEntry(4, 0L)
                .containsEntry(5, 0L);
    }

    @Test
    @DisplayName("查询评价列表应使用正确分页参数")
    void listReviews_shouldUseCorrectPagination() {
        PageResult<ProductRating> page = PageResult.of(List.of(), 0L, 2, 20);
        when(productRatingQueryRepository.findByProductId("10", 2, 20)).thenReturn(page);

        queryHandler.listReviews("10", 2, 20);

        verify(productRatingQueryRepository).findByProductId("10", 2, 20);
    }
}
