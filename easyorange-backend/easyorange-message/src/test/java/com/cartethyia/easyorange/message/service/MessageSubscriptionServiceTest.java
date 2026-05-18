package com.cartethyia.easyorange.message.service;

import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.message.domain.repository.MessageSubscriptionRepository;
import com.cartethyia.easyorange.message.dto.request.SubscriptionRequest;
import com.cartethyia.easyorange.message.dto.vo.MessageSubscriptionVO;
import com.cartethyia.easyorange.message.entity.MessageSubscription;
import com.cartethyia.easyorange.message.service.impl.MessageSubscriptionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageSubscriptionService 单元测试")
class MessageSubscriptionServiceTest {

    @Mock
    private MessageSubscriptionRepository messageSubscriptionRepository;

    @InjectMocks
    private MessageSubscriptionServiceImpl subscriptionService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("getMySubscriptions")
    class GetMySubscriptionsTests {

        @Test
        @DisplayName("获取当前用户的订阅列表")
        void getMySubscriptions_returnsSubscriptions() {
            MessageSubscription sub1 = MessageSubscription.create(USER_ID, "SYSTEM", "WEBSOCKET", true);
            MessageSubscription sub2 = MessageSubscription.create(USER_ID, "MARKETING", "EMAIL", false);

            when(messageSubscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(sub1, sub2));

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                List<MessageSubscriptionVO> result = subscriptionService.getMySubscriptions();

                assertThat(result).hasSize(2);
                assertThat(result.get(0).getMessageType()).isEqualTo("SYSTEM");
                assertThat(result.get(0).getPushChannel()).isEqualTo("WEBSOCKET");
                assertThat(result.get(0).getEnabled()).isTrue();
                assertThat(result.get(1).getMessageType()).isEqualTo("MARKETING");
                assertThat(result.get(1).getEnabled()).isFalse();
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("无订阅时返回空列表")
        void getMySubscriptions_noSubscriptions_returnsEmpty() {
            when(messageSubscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of());

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                List<MessageSubscriptionVO> result = subscriptionService.getMySubscriptions();

                assertThat(result).isEmpty();
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("updateSubscription")
    class UpdateSubscriptionTests {

        @Test
        @DisplayName("更新已有订阅为启用")
        void updateSubscription_existing_enable() {
            SubscriptionRequest request = new SubscriptionRequest();
            request.setMessageType("SYSTEM");
            request.setPushChannel("WEBSOCKET");
            request.setEnabled(true);

            MessageSubscription existing = MessageSubscription.create(USER_ID, "SYSTEM", "WEBSOCKET", false);

            when(messageSubscriptionRepository.findByUserIdAndTypeAndChannel(USER_ID, "SYSTEM", "WEBSOCKET"))
                    .thenReturn(existing);

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                subscriptionService.updateSubscription(request);

                assertThat(existing.isEnabled()).isTrue();
                verify(messageSubscriptionRepository).update(existing);
                verify(messageSubscriptionRepository, never()).save(any());
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("更新已有订阅为禁用")
        void updateSubscription_existing_disable() {
            SubscriptionRequest request = new SubscriptionRequest();
            request.setMessageType("SYSTEM");
            request.setPushChannel("WEBSOCKET");
            request.setEnabled(false);

            MessageSubscription existing = MessageSubscription.create(USER_ID, "SYSTEM", "WEBSOCKET", true);

            when(messageSubscriptionRepository.findByUserIdAndTypeAndChannel(USER_ID, "SYSTEM", "WEBSOCKET"))
                    .thenReturn(existing);

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                subscriptionService.updateSubscription(request);

                assertThat(existing.isEnabled()).isFalse();
                verify(messageSubscriptionRepository).update(existing);
                verify(messageSubscriptionRepository, never()).save(any());
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("新建不存在的订阅")
        void updateSubscription_notExists_createsNew() {
            SubscriptionRequest request = new SubscriptionRequest();
            request.setMessageType("SYSTEM");
            request.setPushChannel("WEBSOCKET");
            request.setEnabled(true);

            when(messageSubscriptionRepository.findByUserIdAndTypeAndChannel(USER_ID, "SYSTEM", "WEBSOCKET"))
                    .thenReturn(null);

            TestSecurityUtil.setSecurityContext(USER_ID);
            try {
                subscriptionService.updateSubscription(request);

                ArgumentCaptor<MessageSubscription> captor = ArgumentCaptor.forClass(MessageSubscription.class);
                verify(messageSubscriptionRepository).save(captor.capture());
                verify(messageSubscriptionRepository, never()).update(any());

                MessageSubscription saved = captor.getValue();
                assertThat(saved.getUserId()).isEqualTo(USER_ID);
                assertThat(saved.getMessageType()).isEqualTo("SYSTEM");
                assertThat(saved.getPushChannel()).isEqualTo("WEBSOCKET");
                assertThat(saved.isEnabled()).isTrue();
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }

    @Nested
    @DisplayName("isSubscribed")
    class IsSubscribedTests {

        @Test
        @DisplayName("用户已订阅时返回 true")
        void isSubscribed_enabled_returnsTrue() {
            when(messageSubscriptionRepository.existsEnabled(USER_ID, "SYSTEM", "WEBSOCKET")).thenReturn(true);

            boolean result = subscriptionService.isSubscribed(USER_ID, "SYSTEM", "WEBSOCKET");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("用户未订阅时返回 false")
        void isSubscribed_disabled_returnsFalse() {
            when(messageSubscriptionRepository.existsEnabled(USER_ID, "SYSTEM", "WEBSOCKET")).thenReturn(false);

            boolean result = subscriptionService.isSubscribed(USER_ID, "SYSTEM", "WEBSOCKET");

            assertThat(result).isFalse();
        }
    }
}
