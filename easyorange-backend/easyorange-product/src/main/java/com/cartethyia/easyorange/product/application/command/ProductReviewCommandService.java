package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductReviewDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewCommandService {

    private final ProductReviewMapper reviewMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long createReview(CreateProductReviewCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        ProductReviewDO review = ProductReviewDO.builder()
                .productId(command.getProductId())
                .userId(userId)
                .rating(command.getRating())
                .content(command.getContent())
                .likes(0)
                .status(1)
                .build();

        reviewMapper.insert(review);

        log.info("action=create_review reviewId={} productId={} userId={} rating={}",
                review.getId(), command.getProductId(), userId, command.getRating());

        return review.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long reviewId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        ProductReviewDO review = reviewMapper.selectById(reviewId);
        if (review == null || review.getDelFlag() != 0) {
            throw new IllegalArgumentException("评价不存在");
        }

        if (!review.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能删除自己的评价");
        }

        reviewMapper.deleteById(reviewId);

        log.info("action=delete_review reviewId={} userId={}", reviewId, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void likeReview(Long reviewId) {
        reviewMapper.incrementLikes(reviewId);
        log.info("action=like_review reviewId={}", reviewId);
    }
}
