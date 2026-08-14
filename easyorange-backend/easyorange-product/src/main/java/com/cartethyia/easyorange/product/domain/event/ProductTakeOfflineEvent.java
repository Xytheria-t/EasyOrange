package com.cartethyia.easyorange.product.domain.event;

public record ProductTakeOfflineEvent(String eventId, String productId, String sellerId) implements ProductEvent {}
