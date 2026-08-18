package com.cartethyia.easyorange.favorite.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record Favorite(String id, String userId, String productId, BigDecimal priceSnapshot, LocalDateTime createTime) {

    public static Favorite create(String userId, String productId, BigDecimal price) {
        if (userId == null || userId.isBlank()) {
            throw BusinessException.of("userId 不能为空");
        }
        if (productId == null || productId.isBlank()) {
            throw BusinessException.of("productId 不能为空");
        }
        if (price == null || price.signum() < 0) {
            throw BusinessException.of("收藏时必须记录商品价格快照");
        }
        return new Favorite(null, userId, productId, price, LocalDateTime.now());
    }

    public static Favorite reconstitute(
            String id, String userId, String productId, BigDecimal priceSnapshot, LocalDateTime createTime) {
        return new Favorite(id, userId, productId, priceSnapshot, createTime);
    }

    public void validateOwnership(String userId) {
        if (!Objects.equals(this.userId, userId)) {
            throw BusinessException.of("无权操作他人的收藏");
        }
    }

    /** 是否降价：有价格快照且新价低于快照价（快照为空视为未知，不判定降价）。 */
    public boolean isPriceDrop(BigDecimal newPrice) {
        return priceSnapshot != null && newPrice != null && newPrice.compareTo(priceSnapshot) < 0;
    }

    /** 降价通知后更新快照为最新价格，保证只提醒"再创新低"。 */
    public Favorite withPriceSnapshot(BigDecimal newPrice) {
        return new Favorite(id, userId, productId, newPrice, createTime);
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
