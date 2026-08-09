package com.cartethyia.easyorange.favorite.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.Objects;

public record Favorite(String id, String userId, String productId, LocalDateTime createTime) {

    public static Favorite create(FavoriteCreateSpec spec) {
        if (spec.userId() == null || spec.userId().isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (spec.productId() == null || spec.productId().isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        return new Favorite(null, spec.userId(), spec.productId(), LocalDateTime.now());
    }

    public static Favorite reconstitute(String id, String userId, String productId, LocalDateTime createTime) {
        return new Favorite(id, userId, productId, createTime);
    }

    public void validateOwnership(String userId) {
        if (!Objects.equals(this.userId, userId)) {
            throw BusinessException.of("无权操作他人的收藏");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Favorite other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Favorite{id=" + id + ", userId=" + userId + ", productId=" + productId + "}";
    }
}
