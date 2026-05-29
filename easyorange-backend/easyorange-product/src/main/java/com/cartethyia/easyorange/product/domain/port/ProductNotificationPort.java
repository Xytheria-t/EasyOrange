package com.cartethyia.easyorange.product.domain.port;

public interface ProductNotificationPort {

    void notifyProductCreated(Long productId, Long userId);

    void notifyProductMarkedSold(Long productId, Long userId);

    void notifyLowStock(Long productId, int currentStock);
}