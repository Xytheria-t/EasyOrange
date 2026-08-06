package com.cartethyia.easyorange.product.domain.event;

public record ProductTakeOfflineEvent(String productId, String sellerId) implements ProductEvent {}
