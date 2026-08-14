package com.cartethyia.easyorange.product.domain.event;

public record ProductMarkedSoldEvent(String eventId, String productId, String sellerId) implements ProductEvent {}
