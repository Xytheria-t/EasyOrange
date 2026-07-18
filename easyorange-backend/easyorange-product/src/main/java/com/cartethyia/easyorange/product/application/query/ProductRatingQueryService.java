package com.cartethyia.easyorange.product.application.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.dto.PageRequest;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductRatingDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductRatingMapper;
import com.cartethyia.easyorange.product.application.query.ProductRatingVO;
import com.cartethyia.easyorange.product.application.query.RatingStatsVO;
import com.cartethyia.easyorange.product.domain.port.SellerInfoPort;
import com.cartethyia.easyorange.product.domain.valueobject.SellerInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRatingQueryService {

    private final ProductRatingMapper reviewMapper;
    private final SellerInfoPort sellerInfoPort;

    @Transactional(readOnly = true)
    public PageResult<ProductRatingVO> listReviews(String productId, Integer pageNum, Integer pageSize) {
        var pageReq = PageRequest.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();

        Page<ProductRatingDO> page = new Page<>(pageReq.getPageNum(), pageReq.getPageSize());
        LambdaQueryWrapper<ProductRatingDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductRatingDO::getProductId, productId)
                .eq(ProductRatingDO::getStatus, 1)
                .orderByDesc(ProductRatingDO::getCreateTime);

        Page<ProductRatingDO> reviewPage = reviewMapper.selectPage(page, wrapper);

        if (reviewPage.getRecords().isEmpty()) {
            return PageResult.empty(pageReq.getPageNum(), pageReq.getPageSize());
        }

        Map<String, SellerInfo> userMap = resolveUsers(reviewPage.getRecords());

        List<ProductRatingVO> vos = reviewPage.getRecords().stream()
                .map(r -> toReviewVO(r, userMap))
                .collect(Collectors.toList());

        return PageResult.of(vos, reviewPage.getTotal(), pageReq.getPageNum(), pageReq.getPageSize());
    }

    @Transactional(readOnly = true)
    public RatingStatsVO getReviewStats(String productId) {
        LambdaQueryWrapper<ProductRatingDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductRatingDO::getProductId, productId)
                .eq(ProductRatingDO::getStatus, 1);

        List<ProductRatingDO> reviews = reviewMapper.selectList(wrapper);

        long totalCount = reviews.size();
        if (totalCount == 0) {
            return RatingStatsVO.builder()
                    .productId(productId)
                    .totalCount(0L)
                    .averageRating(BigDecimal.ZERO)
                    .ratingDistribution(Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L))
                    .build();
        }

        double avg = reviews.stream()
                .mapToInt(ProductRatingDO::getRating)
                .average()
                .orElse(0.0);

        Map<Integer, Long> distribution = reviews.stream()
                .collect(Collectors.groupingBy(
                        ProductRatingDO::getRating,
                        Collectors.counting()
                ));

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

    private Map<String, SellerInfo> resolveUsers(List<ProductRatingDO> reviews) {
        Set<String> userIds = reviews.stream()
                .map(ProductRatingDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return sellerInfoPort.getSellerInfos(userIds);
    }

    private ProductRatingVO toReviewVO(ProductRatingDO review, Map<String, SellerInfo> userMap) {
        SellerInfo user = userMap.get(review.getUserId());
        return ProductRatingVO.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .userId(review.getUserId())
                .username(user != null ? user.username() : "未知用户")
                .userAvatar(user != null ? user.avatar() : null)
                .rating(review.getRating())
                .content(review.getContent())
                .likes(review.getLikes())
                .status(review.getStatus())
                .createTime(review.getCreateTime())
                .updateTime(review.getUpdateTime())
                .build();
    }
}
