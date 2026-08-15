package com.cartethyia.easyorange.adapter.outbound.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.domain.enums.AdminResultCode;
import com.cartethyia.easyorange.admin.domain.port.AdminRatingPort;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.rating.ProductRatingDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.rating.ProductRatingMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Admin 评价查询适配器
 * 实现 AdminRatingPort，通过 ProductRating Mapper 查询数据并转换为 Admin 模块需要的格式
 */
@Primary
@Component
@RequiredArgsConstructor
public class AdminRatingAdapter implements AdminRatingPort {

    private final ProductRatingMapper ratingMapper;

    @Override
    public RatingQueryResult queryRatings(RatingQueryCondition condition) {
        var wrapper = ChainWrappers.lambdaQueryChain(ratingMapper).eq(ProductRatingDO::getDelFlag, 0);

        if (condition.productId() != null) {
            wrapper.eq(ProductRatingDO::getProductId, condition.productId());
        }
        if (condition.userId() != null) {
            wrapper.eq(ProductRatingDO::getUserId, condition.userId());
        }
        if (condition.rating() != null) {
            wrapper.eq(ProductRatingDO::getRating, condition.rating());
        }
        if (condition.status() != null) {
            wrapper.eq(ProductRatingDO::getStatus, condition.status());
        }
        if (StringUtils.hasText(condition.keyword())) {
            wrapper.like(ProductRatingDO::getContent, condition.keyword());
        }
        if (condition.startTime() != null) {
            wrapper.ge(ProductRatingDO::getCreateTime, condition.startTime());
        }
        if (condition.endTime() != null) {
            wrapper.le(ProductRatingDO::getCreateTime, condition.endTime());
        }

        wrapper.orderByDesc(ProductRatingDO::getCreateTime);

        int pageNum = condition.pageNum() != null ? condition.pageNum() : 1;
        int pageSize = condition.pageSize() != null ? condition.pageSize() : 20;
        Page<ProductRatingDO> page = wrapper.page(new Page<>(pageNum, pageSize));

        List<RatingSummary> records =
                page.getRecords().stream().map(this::toSummary).toList();

        return new RatingQueryResult(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public RatingSummary getRatingDetail(String ratingId) {
        ProductRatingDO review = ratingMapper.selectById(ratingId);
        if (review == null || review.getDelFlag() != 0) {
            return null;
        }
        return toSummary(review);
    }

    @Override
    public void deleteRating(String ratingId) {
        ProductRatingDO review = ratingMapper.selectById(ratingId);
        if (review == null || review.getDelFlag() != 0) {
            throw BusinessException.of(AdminResultCode.RATING_NOT_FOUND_OR_DELETED);
        }
        ratingMapper.deleteById(ratingId);
    }

    private RatingSummary toSummary(ProductRatingDO review) {
        return new RatingSummary(
                review.getId(),
                review.getProductId(),
                review.getUserId(),
                review.getRating(),
                review.getContent(),
                review.getReplyContent(),
                review.getLikes(),
                review.getStatus(),
                review.getCreateTime(),
                review.getUpdateTime());
    }
}
