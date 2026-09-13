package com.cartethyia.easyorange.product.domain.entity;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;
import com.cartethyia.easyorange.product.domain.valueobject.Rating;
import com.cartethyia.easyorange.product.domain.valueobject.ReviewContent;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ProductRating {
    private String id;
    private final String productId;
    private final String userId;
    private final String orderId; // nullable
    private final Rating rating;
    private final ReviewContent content;
    private String replyContent;
    private LocalDateTime replyTime;
    private int likes;
    private int status; // 1=active, 0=deleted
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;

    // Private constructor for factory methods
    private ProductRating(
            String id,
            String productId,
            String userId,
            String orderId,
            Rating rating,
            ReviewContent content,
            String replyContent,
            LocalDateTime replyTime,
            int likes,
            int status,
            LocalDateTime createTime,
            LocalDateTime updateTime) {
        this.id = id;
        this.productId = productId;
        this.userId = userId;
        this.orderId = orderId;
        this.rating = rating;
        this.content = content;
        this.replyContent = replyContent;
        this.replyTime = replyTime;
        this.likes = likes;
        this.status = status;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    /**
     * 创建新评价。
     *
     * @param id 评价 ID，由应用层 {@code IdGenerator} 生成（{@code BaseDO.id} 为 {@code IdType.INPUT}，数据库不回填）
     * @param orderId 成交订单 ID，评价必须落到真实成交订单上（见 {@code CompletedOrderPort}）
     */
    public static ProductRating create(
            String id, String productId, String userId, String orderId, int rating, String content) {
        if (orderId == null || orderId.isBlank()) {
            throw new RatingDomainException(ProductResultCode.RATING_ORDER_REQUIRED);
        }
        var now = LocalDateTime.now();
        return new ProductRating(
                id,
                productId,
                userId,
                orderId,
                Rating.of(rating),
                ReviewContent.of(content),
                null,
                null,
                0,
                1,
                now,
                now);
    }

    /** Reconstitute from persistence */
    public static ProductRating reconstitute(
            String id,
            String productId,
            String userId,
            String orderId,
            int rating,
            String content,
            String replyContent,
            LocalDateTime replyTime,
            int likes,
            int status,
            LocalDateTime createTime,
            LocalDateTime updateTime) {
        return new ProductRating(
                id,
                productId,
                userId,
                orderId,
                Rating.of(rating),
                ReviewContent.of(content),
                replyContent,
                replyTime,
                likes,
                status,
                createTime,
                updateTime);
    }

    /** Like this review */
    public void like() {
        this.likes++;
    }

    /** Soft-delete this review */
    public void delete() {
        this.status = 0;
    }

    /** 评价领域不变量被破坏（如未绑定成交订单）。 */
    public static class RatingDomainException extends BaseBusinessException {

        public RatingDomainException(IResultCode resultCode) {
            super(resultCode);
        }
    }
}
