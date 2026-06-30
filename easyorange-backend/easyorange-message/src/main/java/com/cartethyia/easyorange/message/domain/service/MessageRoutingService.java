package com.cartethyia.easyorange.message.domain.service;

import com.cartethyia.easyorange.message.domain.aggregate.MessageSubscriptionAggregate;
import com.cartethyia.easyorange.message.domain.repository.MessageSubscriptionRepository;
import com.cartethyia.easyorange.message.websocket.WebSocketNotifier;

import java.util.List;

public class MessageRoutingService {

    private final MessageSubscriptionRepository subscriptionRepository;
    private final WebSocketNotifier sessionManager;

    /**
     * Constructs a message routing service with the required dependencies.
     *
     * @param subscriptionRepository repository for looking up message subscriptions
     * @param sessionManager         WebSocket notifier for checking online status
     */
    public MessageRoutingService(MessageSubscriptionRepository subscriptionRepository, WebSocketNotifier sessionManager) {
        this.subscriptionRepository = subscriptionRepository;
        this.sessionManager = sessionManager;
    }

    /**
     * Determines how a message should be routed to the given receiver.
     *
     * @param receiverId the ID of the target user
     * @return a RouteDecision containing whether the user is online and their active subscriptions
     */
    public RouteDecision decideRoute(String receiverId) {
        boolean isOnline = sessionManager.isUserOnline(receiverId);
        List<MessageSubscriptionAggregate> subscriptions = subscriptionRepository.findByUserId(receiverId);
        return new RouteDecision(isOnline, subscriptions);
    }

    public record RouteDecision(boolean isOnline, List<MessageSubscriptionAggregate> subscriptions) {}
}
