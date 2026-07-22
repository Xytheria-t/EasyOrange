package com.cartethyia.easyorange.product.domain.entity;

import com.cartethyia.easyorange.product.domain.valueobject.Rating;
import com.cartethyia.easyorange.product.domain.valueobject.ReviewContent;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ProductRating {
    private String id;
    private final String productId;
    private final String userId;
    private final String orderId;  // nullable
    private final Rating rating;
    private final ReviewContent content;
    private String replyContent;
    private LocalDateTime replyTime;
    private int likes;
    private int status; // 1=active, 0=deleted
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;

    // Private constructor for factory methods
    private ProductRating(String id, String productId, String userId, String orderId,
                          Rating rating, ReviewContent content,
                          String replyContent, LocalDateTime replyTime,
                          int likes, int status,
                          LocalDateTime createTime, LocalDateTime updateTime) {
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

    /** Factory method for creating a new rating */
    public static ProductRating create(String productId, String userId, int rating, String content) {
        var now = LocalDateTime.now();
        return new ProductRating(null, productId, userId, null,
                Rating.of(rating), ReviewContent.of(content),
                null, null, 0, 1, now, now);
    }

    /** Reconstitute from persistence */
    public static ProductRating reconstitute(String id, String productId, String userId, String orderId,
                                              int rating, String content,
                                              String replyContent, LocalDateTime replyTime,
                                              int likes, int status,
                                              LocalDateTime createTime, LocalDateTime updateTime) {
        return new ProductRating(id, productId, userId, orderId,
                Rating.of(rating), ReviewContent.of(content),
                replyContent, replyTime, likes, status, createTime, updateTime);
    }

    /** Assign ID after persistence */
    public ProductRating assignId(String id) {
        if (this.id != null) return this;
        this.id = id;
        return this;
    }

    /** Like this review */
    public void like() {
        this.likes++;
    }

    /** Soft-delete this review */
    public void delete() {
        this.status = 0;
    }
}
