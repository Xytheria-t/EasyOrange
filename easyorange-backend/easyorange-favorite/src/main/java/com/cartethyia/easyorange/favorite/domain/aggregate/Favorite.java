package com.cartethyia.easyorange.favorite.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.Objects;

public class Favorite {

    private final String id;
    private final String userId;
    private final String productId;
    private final LocalDateTime createTime;

    private Favorite(String id, String userId, String productId, LocalDateTime createTime) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.createTime = createTime;
    }

    public static Favorite create(String userId, String productId) {
        if (userId == null || productId == null) {
            throw new IllegalArgumentException("userId and productId must not be null");
        }
        return new Favorite(null, userId, productId, LocalDateTime.now());
    }

    public static Favorite reconstitute(String id, String userId, String productId, LocalDateTime createTime) {
        return new Favorite(id, userId, productId, createTime);
    }

    public boolean belongsTo(String userId) {
        return Objects.equals(this.userId, userId);
    }

    public void validateOwnership(String userId) {
        if (!belongsTo(userId)) {
            throw BusinessException.of("无权操作他人的收藏");
        }
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getProductId() { return productId; }
    public LocalDateTime getCreateTime() { return createTime; }
}
