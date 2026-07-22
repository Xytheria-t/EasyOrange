package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.domain.entity.ProductRating;
import com.cartethyia.easyorange.product.domain.repository.ProductRatingRepository;
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
public class ProductRatingCommandService {

    public record CreateProductRatingCommand(
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

    private final ProductRatingRepository productRatingRepository;

    @Transactional(rollbackFor = Exception.class)
    public String createReview(CreateProductRatingCommand command) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        ProductRating rating = ProductRating.create(command.productId(), userId, command.rating(), command.content());
        productRatingRepository.save(rating);

        log.info("action=create_review reviewId={} productId={} userId={} rating={}",
                rating.getId(), command.productId(), userId, command.rating());

        return rating.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(String reviewId) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        ProductRating rating = productRatingRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("评价不存在"));

        if (!rating.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能删除自己的评价");
        }

        rating.delete();
        productRatingRepository.update(rating);

        log.info("action=delete_review reviewId={} userId={}", reviewId, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void likeReview(String reviewId) {
        ProductRating rating = productRatingRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("评价不存在"));
        rating.like();
        productRatingRepository.update(rating);
        log.info("action=like_review reviewId={}", reviewId);
    }
}
