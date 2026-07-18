package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record ProductTakeOfflineEvent(String productId, String sellerId) implements DomainEvent {
}
