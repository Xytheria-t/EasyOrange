package com.cartethyia.easyorange.favorite.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.Objects;

public class Favorite {

    private final Long id;
    private final Long userId;
    private final Long productId;
    private final LocalDateTime createTime;

    private Favorite(Long id, Long userId, Long productId, LocalDateTime createTime) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.createTime = createTime;
    }

    public static Favorite create(Long userId, Long productId) {
        if (userId == null || productId == null) {
            throw new IllegalArgumentException("userId and productId must not be null");
        }
        return new Favorite(null, userId, productId, LocalDateTime.now());
    }

    public static Favorite reconstitute(Long id, Long userId, Long productId, LocalDateTime createTime) {
        return new Favorite(id, userId, productId, createTime);
    }

    public boolean belongsTo(Long userId) {
        return Objects.equals(this.userId, userId);
    }

    public void validateOwnership(Long userId) {
        if (!belongsTo(userId)) {
            throw BusinessException.of("无权操作他人的收藏");
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getProductId() { return productId; }
    public LocalDateTime getCreateTime() { return createTime; }
}
