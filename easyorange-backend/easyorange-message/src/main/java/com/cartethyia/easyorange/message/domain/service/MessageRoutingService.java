package com.cartethyia.easyorange.message.domain.service;

import com.cartethyia.easyorange.message.domain.aggregate.MessageSubscriptionAggregate;
import com.cartethyia.easyorange.message.domain.repository.MessageSubscriptionRepository;
import com.cartethyia.easyorange.message.websocket.WebSocketNotifier;

import java.util.List;

public class MessageRoutingService {

    private final MessageSubscriptionRepository subscriptionRepository;
    private final WebSocketNotifier sessionManager;

    public MessageRoutingService(MessageSubscriptionRepository subscriptionRepository, WebSocketNotifier sessionManager) {
        this.subscriptionRepository = subscriptionRepository;
        this.sessionManager = sessionManager;
    }

    public RouteDecision decideRoute(Long receiverId) {
        boolean isOnline = sessionManager.isUserOnline(receiverId);
        List<MessageSubscriptionAggregate> subscriptions = subscriptionRepository.findByUserId(receiverId);
        return new RouteDecision(isOnline, subscriptions);
    }

    public record RouteDecision(boolean isOnline, List<MessageSubscriptionAggregate> subscriptions) {}
}
