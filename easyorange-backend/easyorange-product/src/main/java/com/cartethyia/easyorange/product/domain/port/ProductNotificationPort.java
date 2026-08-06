package com.cartethyia.easyorange.product.domain.port;

public interface ProductNotificationPort {

    void notifyProductCreated(String productId, String userId);

    void notifyProductMarkedSold(String productId, String userId);

    void notifyLowStock(String productId, String sellerId, int currentStock);
}
