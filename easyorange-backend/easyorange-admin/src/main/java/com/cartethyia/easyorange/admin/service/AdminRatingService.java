package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminRatingAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminRatingDeleteRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminRatingQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminRatingResponse;
import com.cartethyia.easyorange.admin.domain.enums.AdminResultCode;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminRatingPort;
import com.cartethyia.easyorange.admin.domain.port.AdminRatingPort.RatingQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminRatingPort.RatingQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminRatingPort.RatingSummary;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserInfo;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
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

    private final AdminRatingPort adminRatingPort;
    private final AdminUserPort adminUserPort;
    private final AdminProductPort adminProductPort;
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

        RatingQueryResult result = adminRatingPort.queryRatings(condition);
        if (result.records().isEmpty()) {
            return PageResult.empty(result.pageNum(), result.pageSize());
        }

        List<String> productIds = result.records().stream()
                .map(RatingSummary::productId)
                .distinct()
                .toList();
        List<String> userIds =
                result.records().stream().map(RatingSummary::userId).distinct().toList();

        Map<String, UserInfo> userMap = adminUserPort.getUserInfos(userIds);
        Map<String, ProductInfo> productMap = adminProductPort.getProductInfos(productIds);

        List<AdminRatingResponse> records = result.records().stream()
                .map(r -> assembler.toAdminRatingResponse(r, userMap.get(r.userId()), productMap.get(r.productId())))
                .toList();

        return PageResult.of(records, result.total(), result.pageNum(), result.pageSize());
    }

    @Transactional(readOnly = true)
    public AdminRatingResponse getReviewDetail(String id) {
        RatingSummary review = adminRatingPort.getRatingDetail(id);
        BizRequire.notNull(review, AdminResultCode.RATING_NOT_FOUND);

        UserInfo user = adminUserPort.getUserInfos(List.of(review.userId())).get(review.userId());
        ProductInfo product =
                adminProductPort.getProductInfos(List.of(review.productId())).get(review.productId());

        return assembler.toAdminRatingResponse(review, user, product);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(String operatorId, String id, AdminRatingDeleteRequest request) {
        adminRatingPort.deleteRating(id);

        log.info("action=admin_delete_review reviewId={} operatorId={} reason={}", id, operatorId, request.getReason());
    }
}
