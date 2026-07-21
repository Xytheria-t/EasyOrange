package com.cartethyia.easyorange.product.domain.event;

public record ProductMarkedSoldEvent(String productId, String sellerId) implements ProductEvent {
}
