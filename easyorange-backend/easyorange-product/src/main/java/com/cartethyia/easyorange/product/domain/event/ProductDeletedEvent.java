package com.cartethyia.easyorange.product.domain.event;

public record ProductDeletedEvent(String productId, String userId) implements ProductEvent {}
