package com.cartethyia.easyorange.message.domain.event;

import com.cartethyia.easyorange.common.event.DomainEvent;

public record MessageSentEvent(String messageId, String senderId, String receiverId, Integer type) implements DomainEvent {}
