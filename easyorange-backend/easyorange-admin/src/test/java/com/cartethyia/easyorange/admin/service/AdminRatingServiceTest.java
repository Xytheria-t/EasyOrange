package com.cartethyia.easyorange.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminRatingAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminRatingDeleteRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminRatingQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminRatingResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminRatingQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminRatingQueryPort.RatingQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminRatingQueryPort.RatingSummary;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminRatingService 单元测试")
class AdminRatingServiceTest {

    @Mock
    private AdminRatingQueryPort adminRatingQueryPort;

    @Mock
    private AdminUserQueryPort adminUserQueryPort;

    @Mock
    private AdminProductQueryPort adminProductQueryPort;

    @Spy
    private AdminRatingAssembler assembler = new AdminRatingAssembler();

    @InjectMocks
    private AdminRatingService reviewService;

    private static final String REVIEW_ID = "100";
    private static final String PRODUCT_ID = "200";
    private static final String USER_ID = "300";

    private RatingSummary createSummary(String id, String productId, String userId, Integer rating, String content) {
        return new RatingSummary(
                id, productId, userId, rating, content, null, 5, 1, LocalDateTime.now(), LocalDateTime.now());
    }

    private AdminUserQueryPort.UserInfo createUser(String id, String username, String nickname) {
        return new AdminUserQueryPort.UserInfo(id, username, nickname, "http://example.com/avatar.png", null);
    }

    @Nested
    @DisplayName("listReviews")
    class ListReviewsTests {

        @Test
        @DisplayName("分页查询返回评价列表")
        void listReviews_returnsPagedResults() {
            AdminRatingQueryRequest request = new AdminRatingQueryRequest();
            request.setPageNum(1);
            request.setPageSize(10);

            RatingSummary review = createSummary(REVIEW_ID, PRODUCT_ID, USER_ID, 5, "好商品");
            when(adminRatingQueryPort.queryRatings(any())).thenReturn(new RatingQueryResult(List.of(review), 1, 1, 10));
            when(adminUserQueryPort.getUserInfos(anyList()))
                    .thenReturn(Map.of(USER_ID, createUser(USER_ID, "testuser", "测试用户")));
            when(adminProductQueryPort.getProductInfos(anyList()))
                    .thenReturn(Map.of(PRODUCT_ID, new AdminProductQueryPort.ProductInfo(PRODUCT_ID, "测试商品")));

            PageResult<AdminRatingResponse> result = reviewService.listReviews(request);

            assertThat(result).isNotNull();
            assertThat(result.records()).hasSize(1);
            assertThat(result.total()).isEqualTo(1);

            AdminRatingResponse vo = result.records().get(0);
            assertThat(vo.reviewId()).isEqualTo(REVIEW_ID);
            assertThat(vo.productName()).isEqualTo("测试商品");
            assertThat(vo.username()).isEqualTo("测试用户");
        }

        @Test
        @DisplayName("空结果返回空分页")
        void listReviews_emptyResult_returnsEmptyPage() {
            AdminRatingQueryRequest request = new AdminRatingQueryRequest();
            request.setPageNum(1);
            request.setPageSize(20);

            when(adminRatingQueryPort.queryRatings(any())).thenReturn(new RatingQueryResult(List.of(), 0, 1, 20));

            PageResult<AdminRatingResponse> result = reviewService.listReviews(request);

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
            RatingSummary review = createSummary(REVIEW_ID, PRODUCT_ID, USER_ID, 4, "还不错");
            when(adminRatingQueryPort.getRatingDetail(REVIEW_ID)).thenReturn(review);
            when(adminUserQueryPort.getUserInfos(anyList()))
                    .thenReturn(Map.of(USER_ID, createUser(USER_ID, "testuser", "测试用户")));
            when(adminProductQueryPort.getProductInfos(anyList()))
                    .thenReturn(Map.of(PRODUCT_ID, new AdminProductQueryPort.ProductInfo(PRODUCT_ID, "测试商品")));

            AdminRatingResponse result = reviewService.getReviewDetail(REVIEW_ID);

            assertThat(result).isNotNull();
            assertThat(result.reviewId()).isEqualTo(REVIEW_ID);
            assertThat(result.productName()).isEqualTo("测试商品");
        }

        @Test
        @DisplayName("评价不存在抛出异常")
        void getReviewDetail_notFound_throwsException() {
            when(adminRatingQueryPort.getRatingDetail("999")).thenReturn(null);

            assertThatThrownBy(() -> reviewService.getReviewDetail("999"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("评价不存在");
        }

        @Test
        @DisplayName("已删除评价抛出异常")
        void getReviewDetail_deleted_throwsException() {
            when(adminRatingQueryPort.getRatingDetail(REVIEW_ID)).thenReturn(null);

            assertThatThrownBy(() -> reviewService.getReviewDetail(REVIEW_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("评价不存在");
        }
    }

    @Nested
    @DisplayName("deleteReview")
    class DeleteReviewTests {

        @Test
        @DisplayName("删除评价委托端口")
        void deleteReview_success() {
            AdminRatingDeleteRequest request = new AdminRatingDeleteRequest();
            request.setReason("违规内容");

            reviewService.deleteReview("1", REVIEW_ID, request);

            verify(adminRatingQueryPort).deleteRating(REVIEW_ID);
        }

        @Test
        @DisplayName("端口抛出评价不存在异常向上传播")
        void deleteReview_notFound_throwsException() {
            org.mockito.Mockito.doThrow(BusinessException.of("评价不存在或已被删除"))
                    .when(adminRatingQueryPort)
                    .deleteRating("999");

            AdminRatingDeleteRequest request = new AdminRatingDeleteRequest();
            request.setReason("test");

            assertThatThrownBy(() -> reviewService.deleteReview("1", "999", request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("评价不存在或已被删除");
        }
    }
}
