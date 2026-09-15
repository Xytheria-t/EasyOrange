package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.product.domain.entity.ProductRating;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;
import com.cartethyia.easyorange.product.domain.exception.ProductDomainException;
import com.cartethyia.easyorange.product.domain.port.CompletedOrderPort;
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
    private final IdGenerator idGenerator;
    private final CompletedOrderPort completedOrderPort;

    /**
     * 创建评价 —— 仅成交完成的买家可评价，且一笔订单只评价一次。
     * <p>
     * 订单号由服务端按 (买家, 资产) 反查已完成订单得到，不接受客户端传入，避免越权绑定任意订单；
     * 评价落库后进入 AI 信用画像的评分口径（{@code review_avg_rating}），故必须绑定真实成交。
     *
     * @return 评价 ID
     * @throws BusinessException 无已完成订单（B2016）或该订单已评价（B2017）
     */
    @Transactional(rollbackFor = Exception.class)
    public String createReview(String userId, CreateProductRatingCommand command) {
        String orderId = completedOrderPort
                .findCompletedOrderId(userId, command.productId())
                .orElseThrow(() -> BusinessException.of(ProductResultCode.RATING_ORDER_NOT_COMPLETED));
        if (productRatingRepository.existsByUserIdAndOrderId(userId, orderId)) {
            throw BusinessException.of(ProductResultCode.RATING_ALREADY_EXISTS);
        }

        ProductRating rating = ProductRating.create(
                idGenerator.generateId(), command.productId(), userId, orderId, command.rating(), command.content());
        productRatingRepository.save(rating);

        log.info(
                "action=create_review reviewId={} orderId={} productId={} userId={} rating={}",
                rating.getId(),
                orderId,
                command.productId(),
                userId,
                command.rating());

        return rating.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(String userId, String reviewId) {
        ProductRating rating = productRatingRepository
                .findById(reviewId)
                .orElseThrow(() -> ProductDomainException.ratingNotFound(reviewId));

        if (!rating.getUserId().equals(userId)) {
            throw ProductDomainException.ratingNotOwner(reviewId);
        }

        rating.delete();
        productRatingRepository.update(rating);

        log.info("action=delete_review reviewId={} userId={}", reviewId, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void likeReview(String reviewId) {
        ProductRating rating = productRatingRepository
                .findById(reviewId)
                .orElseThrow(() -> ProductDomainException.ratingNotFound(reviewId));
        rating.like();
        productRatingRepository.update(rating);
        log.info("action=like_review reviewId={}", reviewId);
    }
}
