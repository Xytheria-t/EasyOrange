package com.cartethyia.easyorange.product.application.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductReviewDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductReviewMapper;
import com.cartethyia.easyorange.product.application.query.dto.ProductReviewVO;
import com.cartethyia.easyorange.product.application.query.dto.ReviewStatsVO;
import com.cartethyia.easyorange.product.domain.port.SellerInfoPort;
import com.cartethyia.easyorange.product.domain.valueobject.SellerInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewQueryService 测试")
class ProductReviewQueryServiceTest {

    @Mock
    private ProductReviewMapper reviewMapper;

    @Mock
    private SellerInfoPort sellerInfoPort;

    private ProductReviewQueryService queryService;

    private ProductReviewDO reviewDO;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        queryService = new ProductReviewQueryService(reviewMapper, sellerInfoPort);

        reviewDO = new ProductReviewDO();
        reviewDO.setId(100L);
        reviewDO.setProductId(10L);
        reviewDO.setUserId(1L);
        reviewDO.setRating(5);
        reviewDO.setContent("非常好");
        reviewDO.setLikes(3);
        reviewDO.setStatus(1);
        reviewDO.setCreateTime(LocalDateTime.now());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("查询评价列表应返回分页结果")
    void listReviews_shouldReturnPage() {
        Page<ProductReviewDO> page = new Page<>(1, 10);
        page.setRecords(List.of(reviewDO));
        page.setTotal(1);
        when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(sellerInfoPort.getSellerInfos(anySet())).thenReturn(Map.of(
                1L, new SellerInfo(1L, "认领方", null, "http://avatar.jpg")
        ));

        PageResult<ProductReviewVO> result = queryService.listReviews(10L, 1, 10);

        assertThat(result).isNotNull();
        assertThat(result.records()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
        ProductReviewVO vo = result.records().get(0);
        assertThat(vo.getId()).isEqualTo(100L);
        assertThat(vo.getProductId()).isEqualTo(10L);
        assertThat(vo.getRating()).isEqualTo(5);
        assertThat(vo.getContent()).isEqualTo("非常好");
        assertThat(vo.getLikes()).isEqualTo(3);
        assertThat(vo.getUsername()).isEqualTo("认领方");
        assertThat(vo.getUserAvatar()).isEqualTo("http://avatar.jpg");
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("评价列表为空时应返回空分页")
    void listReviews_withEmptyResult_shouldReturnEmptyPage() {
        Page<ProductReviewDO> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<ProductReviewVO> result = queryService.listReviews(10L, 1, 10);

        assertThat(result.records()).isEmpty();
        assertThat(result.total()).isZero();
        verify(sellerInfoPort, never()).getSellerInfos(anySet());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("用户不存在时应使用默认用户名")
    void listReviews_whenUserNotFound_shouldUseDefault() {
        Page<ProductReviewDO> page = new Page<>(1, 10);
        page.setRecords(List.of(reviewDO));
        page.setTotal(1);
        when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(sellerInfoPort.getSellerInfos(anySet())).thenReturn(Map.of());

        PageResult<ProductReviewVO> result = queryService.listReviews(10L, 1, 10);

        assertThat(result.records().get(0).getUsername()).isEqualTo("未知用户");
        assertThat(result.records().get(0).getUserAvatar()).isNull();
    }

    @Test
    @DisplayName("获取评价统计信息应正确计算")
    void getReviewStats_shouldCalculateCorrectly() {
        ProductReviewDO review2 = new ProductReviewDO();
        review2.setProductId(10L);
        review2.setRating(4);
        review2.setStatus(1);
        ProductReviewDO review3 = new ProductReviewDO();
        review3.setProductId(10L);
        review3.setRating(5);
        review3.setStatus(1);

        when(reviewMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(reviewDO, review2, review3));

        ReviewStatsVO stats = queryService.getReviewStats(10L);

        assertThat(stats.getProductId()).isEqualTo(10L);
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
        when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        ReviewStatsVO stats = queryService.getReviewStats(10L);

        assertThat(stats.getProductId()).isEqualTo(10L);
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
    @SuppressWarnings("unchecked")
    @DisplayName("查询评价列表应使用正确分页参数")
    void listReviews_shouldUseCorrectPagination() {
        Page<ProductReviewDO> page = new Page<>(2, 20);
        page.setRecords(List.of());
        page.setTotal(0);
        when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        queryService.listReviews(10L, 2, 20);

        ArgumentCaptor<Page<ProductReviewDO>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(reviewMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(20);
    }
}
