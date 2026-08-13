package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.product.domain.entity.ProductRating;
import com.cartethyia.easyorange.product.domain.exception.RatingNotFoundException;
import com.cartethyia.easyorange.product.domain.exception.RatingNotOwnerException;
import com.cartethyia.easyorange.product.domain.repository.ProductRatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRatingCommandHandler {

    private final ProductRatingRepository productRatingRepository;

    @Transactional(rollbackFor = Exception.class)
    public String createReview(String userId, CreateProductRatingCommand command) {
        ProductRating rating = ProductRating.create(command.productId(), userId, command.rating(), command.content());
        productRatingRepository.save(rating);

        log.info(
                "action=create_review reviewId={} productId={} userId={} rating={}",
                rating.getId(),
                command.productId(),
                userId,
                command.rating());

        return rating.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(String userId, String reviewId) {
        ProductRating rating =
                productRatingRepository.findById(reviewId).orElseThrow(() -> new RatingNotFoundException(reviewId));

        if (!rating.getUserId().equals(userId)) {
            throw new RatingNotOwnerException(reviewId);
        }

        rating.delete();
        productRatingRepository.update(rating);

        log.info("action=delete_review reviewId={} userId={}", reviewId, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void likeReview(String reviewId) {
        ProductRating rating =
                productRatingRepository.findById(reviewId).orElseThrow(() -> new RatingNotFoundException(reviewId));
        rating.like();
        productRatingRepository.update(rating);
        log.info("action=like_review reviewId={}", reviewId);
    }
}
