package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.common.dto.PageRequest;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.port.query.ProductRatingQueryRepository;
import com.cartethyia.easyorange.product.application.query.dto.ProductRatingVO;
import com.cartethyia.easyorange.product.application.query.dto.RatingStatsVO;
import com.cartethyia.easyorange.product.domain.entity.ProductRating;
import com.cartethyia.easyorange.product.domain.port.SellerInfoPort;
import com.cartethyia.easyorange.product.domain.valueobject.SellerInfo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRatingQueryHandler {

    private final ProductRatingQueryRepository productRatingQueryRepository;
    private final SellerInfoPort sellerInfoPort;

    @Transactional(readOnly = true)
    public PageResult<ProductRatingVO> listReviews(String productId, Integer pageNum, Integer pageSize) {
        var pageReq = PageRequest.builder().pageNum(pageNum).pageSize(pageSize).build();

        PageResult<ProductRating> ratingPage =
                productRatingQueryRepository.findByProductId(productId, pageReq.getPageNum(), pageReq.getPageSize());

        if (ratingPage.records().isEmpty()) {
            return PageResult.empty(pageReq.getPageNum(), pageReq.getPageSize());
        }

        Map<String, SellerInfo> userMap = resolveUsers(ratingPage.records());

        List<ProductRatingVO> vos =
                ratingPage.records().stream().map(r -> toReviewVO(r, userMap)).collect(Collectors.toList());

        return PageResult.of(vos, ratingPage.total(), pageReq.getPageNum(), pageReq.getPageSize());
    }

    @Transactional(readOnly = true)
    public RatingStatsVO getReviewStats(String productId) {
        List<ProductRating> reviews = productRatingQueryRepository.findAllByProductId(productId);

        long totalCount = reviews.size();
        if (totalCount == 0) {
            return RatingStatsVO.builder()
                    .productId(productId)
                    .totalCount(0L)
                    .averageRating(BigDecimal.ZERO)
                    .ratingDistribution(Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L))
                    .build();
        }

        double avg =
                reviews.stream().mapToInt(r -> r.getRating().value()).average().orElse(0.0);

        Map<Integer, Long> distribution = reviews.stream()
                .collect(Collectors.groupingBy(r -> r.getRating().value(), Collectors.counting()));

        for (int i = 1; i <= 5; i++) {
            distribution.putIfAbsent(i, 0L);
        }

        return RatingStatsVO.builder()
                .productId(productId)
                .totalCount(totalCount)
                .averageRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP))
                .ratingDistribution(distribution)
                .build();
    }

    private Map<String, SellerInfo> resolveUsers(List<ProductRating> reviews) {
        Set<String> userIds = reviews.stream()
                .map(ProductRating::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return sellerInfoPort.getSellerInfos(userIds);
    }

    private ProductRatingVO toReviewVO(ProductRating review, Map<String, SellerInfo> userMap) {
        SellerInfo user = userMap.get(review.getUserId());
        return ProductRatingVO.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .userId(review.getUserId())
                .username(user != null ? user.username() : "未知用户")
                .userAvatar(user != null ? user.avatar() : null)
                .rating(review.getRating().value())
                .content(review.getContent().value())
                .likes(review.getLikes())
                .status(review.getStatus())
                .createTime(review.getCreateTime())
                .updateTime(review.getUpdateTime())
                .build();
    }
}
