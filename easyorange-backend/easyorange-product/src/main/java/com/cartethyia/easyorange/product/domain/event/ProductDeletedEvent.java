package com.cartethyia.easyorange.product.domain.event;

public record ProductDeletedEvent(String eventId, String productId, String userId) implements ProductEvent {}
