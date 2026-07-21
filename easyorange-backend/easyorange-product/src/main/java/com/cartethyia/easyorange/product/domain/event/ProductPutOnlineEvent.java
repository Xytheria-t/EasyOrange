package com.cartethyia.easyorange.product.domain.event;

public record ProductPutOnlineEvent(String productId, String sellerId) implements ProductEvent {
}
