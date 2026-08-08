package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminRatingAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminRatingDeleteRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminRatingQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminRatingResponse;
import com.cartethyia.easyorange.admin.domain.enums.AdminResultCode;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminRatingQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminRatingQueryPort.RatingQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminRatingQueryPort.RatingQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminRatingQueryPort.RatingSummary;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort.UserInfo;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRatingService {

    private final AdminRatingQueryPort adminRatingQueryPort;
    private final AdminUserQueryPort adminUserQueryPort;
    private final AdminProductQueryPort adminProductQueryPort;
    private final AdminRatingAssembler assembler;

    @Transactional(readOnly = true)
    public PageResult<AdminRatingResponse> listReviews(AdminRatingQueryRequest request) {
        RatingQueryCondition condition = new RatingQueryCondition(
                request.getProductId(),
                request.getUserId(),
                request.getRating(),
                request.getStatus(),
                request.getKeyword(),
                request.getStartTime(),
                request.getEndTime(),
                request.getPageNum(),
                request.getPageSize());

        RatingQueryResult result = adminRatingQueryPort.queryRatings(condition);
        if (result.records().isEmpty()) {
            return PageResult.empty(result.pageNum(), result.pageSize());
        }

        List<String> productIds = result.records().stream()
                .map(RatingSummary::productId)
                .distinct()
                .toList();
        List<String> userIds =
                result.records().stream().map(RatingSummary::userId).distinct().toList();

        Map<String, UserInfo> userMap = adminUserQueryPort.getUserInfos(userIds);
        Map<String, ProductInfo> productMap = adminProductQueryPort.getProductInfos(productIds);

        List<AdminRatingResponse> records = result.records().stream()
                .map(r -> assembler.toAdminRatingResponse(r, userMap.get(r.userId()), productMap.get(r.productId())))
                .toList();

        return PageResult.of(records, result.total(), result.pageNum(), result.pageSize());
    }

    @Transactional(readOnly = true)
    public AdminRatingResponse getReviewDetail(String id) {
        RatingSummary review = adminRatingQueryPort.getRatingDetail(id);
        BizRequire.notNull(review, AdminResultCode.RATING_NOT_FOUND);

        UserInfo user =
                adminUserQueryPort.getUserInfos(List.of(review.userId())).get(review.userId());
        ProductInfo product = adminProductQueryPort
                .getProductInfos(List.of(review.productId()))
                .get(review.productId());

        return assembler.toAdminRatingResponse(review, user, product);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(String id, AdminRatingDeleteRequest request) {
        adminRatingQueryPort.deleteRating(id);

        String operatorId = SecurityContextUtil.getCurrentUserIdOrThrow();
        log.info("action=admin_delete_review reviewId={} operatorId={} reason={}", id, operatorId, request.getReason());
    }
}
