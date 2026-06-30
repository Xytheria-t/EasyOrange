package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminReviewDeleteRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminReviewQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminReviewResponse;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.admin.util.BatchQueryUtil;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductReviewDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductReviewMapper;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminReviewService 单元测试")
class AdminReviewServiceTest {

    @Mock
    private ProductReviewMapper reviewMapper;

    @Mock
    private BatchQueryUtil batchQueryUtil;

    @InjectMocks
    private AdminReviewService reviewService;

    private static final String REVIEW_ID = "100";
    private static final String PRODUCT_ID = "200";
    private static final String USER_ID = "300";

    private ProductReviewDO createReview(String id, String productId, String userId, Integer rating, String content, Integer delFlag) {
        return ProductReviewDO.builder()
            .id(id)
            .productId(productId)
            .userId(userId)
            .rating(rating)
            .content(content)
            .likes(5)
            .status(1)
            .delFlag(delFlag)
            .createTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .build();
    }

    private ProductDO createProduct(String id, String name) {
        ProductDO product = ProductDO.builder()
            .id(id)
            .name(name)
            .userId(USER_ID)
            .categoryId("1")
            .price(new BigDecimal("99.00"))
            .stock(10)
            .status(1)
            .viewCount(100)
            .conditionLevel(1)
            .build();
        product.setDelFlag(0);
        return product;
    }

    private UserEntity createUser(String id, String username, String nickname) {
        return UserEntity.builder()
            .id(id)
            .username(username)
            .nickName(nickname)
            .avatar("http://example.com/avatar.png")
            .build();
    }

    @Nested
    @DisplayName("listReviews")
    class ListReviewsTests {

        @Test
        @DisplayName("分页查询返回评价列表")
        void listReviews_returnsPagedResults() {
            AdminReviewQueryRequest request = new AdminReviewQueryRequest();
            request.setPageNum(1);
            request.setPageSize(10);

            ProductReviewDO review = createReview(REVIEW_ID, PRODUCT_ID, USER_ID, 5, "好商品", 0);
            Page<ProductReviewDO> pageResult = new Page<>(1, 10, 1);
            pageResult.setRecords(List.of(review));

            when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(pageResult);
            when(batchQueryUtil.batchGetProducts(anyList())).thenReturn(Map.of(PRODUCT_ID, createProduct(PRODUCT_ID, "测试商品")));
            when(batchQueryUtil.batchGetUsers(anyList())).thenReturn(Map.of(USER_ID, createUser(USER_ID, "testuser", "测试用户")));

            PageResult<AdminReviewResponse> result = reviewService.listReviews(request);

            assertThat(result).isNotNull();
            assertThat(result.records()).hasSize(1);
            assertThat(result.total()).isEqualTo(1);

            AdminReviewResponse vo = result.records().get(0);
            assertThat(vo.reviewId()).isEqualTo(REVIEW_ID);
            assertThat(vo.productName()).isEqualTo("测试商品");
            assertThat(vo.username()).isEqualTo("测试用户");
        }

        @Test
        @DisplayName("空结果返回空分页")
        void listReviews_emptyResult_returnsEmptyPage() {
            AdminReviewQueryRequest request = new AdminReviewQueryRequest();
            request.setPageNum(1);
            request.setPageSize(20);

            Page<ProductReviewDO> emptyPage = new Page<>(1, 20, 0);
            emptyPage.setRecords(List.of());

            when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(emptyPage);

            PageResult<AdminReviewResponse> result = reviewService.listReviews(request);

            assertThat(result).isNotNull();
            assertThat(result.records()).isEmpty();
            assertThat(result.total()).isZero();
        }
    }

    @Nested
    @DisplayName("getReviewDetail")
    class GetReviewDetailTests {

        @Test
        @DisplayName("查询评价详情成功")
        void getReviewDetail_returnsDetail() {
            ProductReviewDO review = createReview(REVIEW_ID, PRODUCT_ID, USER_ID, 4, "还不错", 0);

            when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);
            when(batchQueryUtil.batchGetProducts(anyList())).thenReturn(Map.of(PRODUCT_ID, createProduct(PRODUCT_ID, "测试商品")));
            when(batchQueryUtil.batchGetUsers(anyList())).thenReturn(Map.of(USER_ID, createUser(USER_ID, "testuser", "测试用户")));

            AdminReviewResponse result = reviewService.getReviewDetail(REVIEW_ID);

            assertThat(result).isNotNull();
            assertThat(result.reviewId()).isEqualTo(REVIEW_ID);
            assertThat(result.productName()).isEqualTo("测试商品");
        }

        @Test
        @DisplayName("评价不存在抛出异常")
        void getReviewDetail_notFound_throwsException() {
            when(reviewMapper.selectById("999")).thenReturn(null);

            assertThatThrownBy(() -> reviewService.getReviewDetail("999"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("评价不存在");
        }

        @Test
        @DisplayName("已删除评价抛出异常")
        void getReviewDetail_deleted_throwsException() {
            ProductReviewDO review = createReview(REVIEW_ID, PRODUCT_ID, USER_ID, 3, "已删除", 2);
            when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);

            assertThatThrownBy(() -> reviewService.getReviewDetail(REVIEW_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("评价不存在");
        }
    }

    @Nested
    @DisplayName("deleteReview")
    class DeleteReviewTests {

        @Test
        @DisplayName("删除评价成功")
        void deleteReview_success() {
            ProductReviewDO review = createReview(REVIEW_ID, PRODUCT_ID, USER_ID, 3, "要删除的", 0);
            AdminReviewDeleteRequest request = new AdminReviewDeleteRequest();
            request.setReason("违规内容");

            when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);
            TestSecurityUtil.setSecurityContext(1L);
            try {

                reviewService.deleteReview(REVIEW_ID, request);

                verify(reviewMapper).deleteById(REVIEW_ID);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("删除不存在的评价抛出异常")
        void deleteReview_notFound_throwsException() {
            AdminReviewDeleteRequest request = new AdminReviewDeleteRequest();
            request.setReason("test");

            when(reviewMapper.selectById("999")).thenReturn(null);

            assertThatThrownBy(() -> reviewService.deleteReview("999", request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("评价不存在或已被删除");
        }
    }
}
