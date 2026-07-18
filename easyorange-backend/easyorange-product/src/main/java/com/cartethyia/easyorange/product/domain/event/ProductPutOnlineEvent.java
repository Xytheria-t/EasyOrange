package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record ProductPutOnlineEvent(String productId, String sellerId) implements DomainEvent {
}
