package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductReviewDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductReviewMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewCommandService {

    public record CreateProductReviewCommand(
        @NotNull(message = "商品ID不能为空")
        String productId,

        @NotNull(message = "评分不能为空")
        @Min(value = 1, message = "评分最小为1")
        @Max(value = 5, message = "评分最大为5")
        Integer rating,

        @NotBlank(message = "评价内容不能为空")
        @Size(max = 2000, message = "评价内容最多2000字")
        String content
    ) {}

    private final ProductReviewMapper reviewMapper;

    @Transactional(rollbackFor = Exception.class)
    public String createReview(CreateProductReviewCommand command) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        ProductReviewDO review = ProductReviewDO.builder()
                .productId(command.productId())
                .userId(userId)
                .rating(command.rating())
                .content(command.content())
                .likes(0)
                .status(1)
                .build();

        reviewMapper.insert(review);

        log.info("action=create_review reviewId={} productId={} userId={} rating={}",
                review.getId(), command.productId(), userId, command.rating());

        return review.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(String reviewId) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

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
    public void likeReview(String reviewId) {
        reviewMapper.incrementLikes(reviewId);
        log.info("action=like_review reviewId={}", reviewId);
    }
}
