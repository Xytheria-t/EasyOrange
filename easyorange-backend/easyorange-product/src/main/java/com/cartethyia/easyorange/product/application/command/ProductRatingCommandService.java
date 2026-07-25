package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.domain.entity.ProductRating;
import com.cartethyia.easyorange.product.domain.repository.ProductRatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRatingCommandService {

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
