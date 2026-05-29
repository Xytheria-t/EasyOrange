package com.cartethyia.easyorange.message.domain.service;

import com.cartethyia.easyorange.message.domain.aggregate.MessageSubscriptionAggregate;
import com.cartethyia.easyorange.message.domain.repository.MessageSubscriptionRepository;
import com.cartethyia.easyorange.message.websocket.WebSocketNotifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageRoutingService 单元测试")
class MessageRoutingServiceTest {

    @Mock
    private MessageSubscriptionRepository subscriptionRepository;

    @Mock
    private WebSocketNotifier sessionManager;

    @InjectMocks
    private MessageRoutingService routingService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("decideRoute")
    class DecideRouteTests {

        @Test
        @DisplayName("用户在线时路由决策标记为在线")
        void decideRoute_userOnline_returnsOnline() {
            when(sessionManager.isUserOnline(USER_ID)).thenReturn(true);
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of());

            MessageRoutingService.RouteDecision decision = routingService.decideRoute(USER_ID);

            assertThat(decision.isOnline()).isTrue();
            assertThat(decision.subscriptions()).isEmpty();
        }

        @Test
        @DisplayName("用户离线时路由决策标记为离线")
        void decideRoute_userOffline_returnsOffline() {
            when(sessionManager.isUserOnline(USER_ID)).thenReturn(false);
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of());

            MessageRoutingService.RouteDecision decision = routingService.decideRoute(USER_ID);

            assertThat(decision.isOnline()).isFalse();
            assertThat(decision.subscriptions()).isEmpty();
        }

        @Test
        @DisplayName("用户有订阅偏好时路由决策包含订阅列表")
        void decideRoute_hasSubscriptions_returnsSubscriptions() {
            MessageSubscriptionAggregate sub = MessageSubscriptionAggregate.create(USER_ID, "SYSTEM", "WEBSOCKET", true);
            when(sessionManager.isUserOnline(USER_ID)).thenReturn(true);
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(sub));

            MessageRoutingService.RouteDecision decision = routingService.decideRoute(USER_ID);

            assertThat(decision.isOnline()).isTrue();
            assertThat(decision.subscriptions()).hasSize(1);
            assertThat(decision.subscriptions().get(0).messageType()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("用户无订阅时返回空列表")
        void decideRoute_noSubscriptions_returnsEmptyList() {
            when(sessionManager.isUserOnline(USER_ID)).thenReturn(true);
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of());

            MessageRoutingService.RouteDecision decision = routingService.decideRoute(USER_ID);

            assertThat(decision.isOnline()).isTrue();
            assertThat(decision.subscriptions()).isEmpty();
        }
    }
}
