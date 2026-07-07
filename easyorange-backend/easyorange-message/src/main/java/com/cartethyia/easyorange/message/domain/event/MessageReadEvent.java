package com.cartethyia.easyorange.message.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record MessageReadEvent(String messageId, String readerId) implements DomainEvent {}
