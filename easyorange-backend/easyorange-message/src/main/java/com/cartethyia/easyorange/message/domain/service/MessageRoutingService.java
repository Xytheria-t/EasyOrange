package com.cartethyia.easyorange.message.domain.service;

import com.cartethyia.easyorange.message.domain.repository.MessageSubscriptionRepository;
import com.cartethyia.easyorange.message.entity.MessageSubscription;
import com.cartethyia.easyorange.message.websocket.WebSocketNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageRoutingService {

    private final MessageSubscriptionRepository subscriptionRepository;
    private final WebSocketNotifier sessionManager;

    public RouteDecision decideRoute(Long receiverId) {
        boolean isOnline = sessionManager.isUserOnline(receiverId);
        List<MessageSubscription> subscriptions = subscriptionRepository.findByUserId(receiverId);
        return new RouteDecision(isOnline, subscriptions);
    }

    public record RouteDecision(boolean isOnline, List<MessageSubscription> subscriptions) {}
}
