package com.cartethyia.easyorange.adapter.outbound.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.domain.port.AdminRatingQueryPort.RatingSummary;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.rating.ProductRatingDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.rating.ProductRatingMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminRatingQueryAdapter 单元测试")
class AdminRatingQueryAdapterTest {

    @Mock
    private ProductRatingMapper ratingMapper;

    private AdminRatingQueryAdapter adapter;

    private static final String REVIEW_ID = "100";

    @BeforeEach
    void setUp() {
        adapter = new AdminRatingQueryAdapter(ratingMapper);
    }

    private ProductRatingDO review() {
        ProductRatingDO review = new ProductRatingDO();
        review.setId(REVIEW_ID);
        review.setProductId("200");
        review.setUserId("300");
        review.setRating(5);
        review.setDelFlag(0);
        review.setCreateTime(LocalDateTime.now());
        return review;
    }

    @Test
    @DisplayName("删除评价成功")
    void deleteRating_success() {
        when(ratingMapper.selectById(REVIEW_ID)).thenReturn(review());

        adapter.deleteRating(REVIEW_ID);

        verify(ratingMapper).deleteById(REVIEW_ID);
    }

    @Test
    @DisplayName("删除不存在的评价抛出业务异常")
    void deleteRating_notFound_throws() {
        when(ratingMapper.selectById(REVIEW_ID)).thenReturn(null);

        assertThatThrownBy(() -> adapter.deleteRating(REVIEW_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("评价不存在或已被删除");
    }

    @Test
    @DisplayName("查询评价详情")
    void getRatingDetail_returnsSummary() {
        when(ratingMapper.selectById(REVIEW_ID)).thenReturn(review());

        RatingSummary summary = adapter.getRatingDetail(REVIEW_ID);

        assertThat(summary).isNotNull();
        assertThat(summary.id()).isEqualTo(REVIEW_ID);
        assertThat(summary.rating()).isEqualTo(5);
    }
}
