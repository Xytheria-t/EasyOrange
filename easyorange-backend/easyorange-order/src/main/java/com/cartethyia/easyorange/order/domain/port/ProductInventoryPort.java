package com.cartethyia.easyorange.order.domain.port;

import java.math.BigDecimal;
import java.util.List;

public interface ProductInventoryPort {

    List<ProductSnapshot> getSnapshots(List<String> productIds);

    /**
     * 扣减库存 — 必须携带订单号：它就是库存流水的幂等键，重复投递同一订单不会重复扣减。
     */
    void decreaseStock(String orderId, String productId, int quantity);

    /**
     * 恢复库存 — 与扣减同一幂等键（订单号 + 资产），且数量必须与下单时扣减的一致，
     * 否则取消/退款后库存会永久偏离。
     */
    void restoreStock(String orderId, String productId, int quantity);

    void markAsSold(String productId);

    record ProductSnapshot(String productId, String sellerId, BigDecimal price, boolean isOnline, int stockQuantity) {
        public boolean hasStock() {
            return stockQuantity > 0;
        }
    }
}
