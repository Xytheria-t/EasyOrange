package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.Principal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

@DisplayName("StompSubscribeGuard 单元测试")
class StompSubscribeGuardTest {

    private final StompSubscribeGuard guard = new StompSubscribeGuard();

    private static Principal user(String id) {
        return () -> id;
    }

    private static Message<byte[]> subscribe(String destination, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        if (principal != null) {
            accessor.setUser(principal);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("会话参与者订阅聊天队列 / typing / recall 均放行")
    void participantSubscriptions_allowed() {
        Principal alice = user("1");

        assertThat(guard.preSend(subscribe("/queue/chat/conv_1_2", alice), null))
                .isNotNull();
        assertThat(guard.preSend(subscribe("/topic/chat/conv_1_2/typing", alice), null))
                .isNotNull();
        assertThat(guard.preSend(subscribe("/topic/chat/conv_1_2/recall", alice), null))
                .isNotNull();
    }

    @Test
    @DisplayName("非参与者订阅他人会话被拒（conversationId 可由双方 ID 推导，不拦等于泄露聊天内容）")
    void foreignConversation_rejected() {
        Principal mallory = user("3");

        assertThatThrownBy(() -> guard.preSend(subscribe("/queue/chat/conv_1_2", mallory), null))
                .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> guard.preSend(subscribe("/topic/chat/conv_1_2/recall", mallory), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    @DisplayName("用户目的地由 broker 按会话自绑，直接放行")
    void userDestination_allowed() {
        assertThat(guard.preSend(subscribe("/user/queue/notification", user("1")), null))
                .isNotNull();
        assertThat(guard.preSend(subscribe("/user/queue/unread-count", user("1")), null))
                .isNotNull();
    }

    @Test
    @DisplayName("白名单外目的地 / 非参与者 ID / 未认证一律拒（fail-closed）")
    void whitelistOutside_rejected() {
        Principal alice = user("1");

        // 目的地格式合法但 ID 非本人
        assertThatThrownBy(() -> guard.preSend(subscribe("/queue/chat/garbage", alice), null))
                .isInstanceOf(MessageDeliveryException.class);
        // 白名单外的 topic
        assertThatThrownBy(() -> guard.preSend(subscribe("/topic/orders", alice), null))
                .isInstanceOf(MessageDeliveryException.class);
        // 未认证
        assertThatThrownBy(() -> guard.preSend(subscribe("/user/queue/notification", null), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    @DisplayName("非 SUBSCRIBE 命令原样放行（SEND 归各 Handler 校验）")
    void nonSubscribeCommand_passthrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat.send");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThat(guard.preSend(message, null)).isSameAs(message);
    }

    @Test
    @DisplayName("系统伪会话（conv_自己_system）对本人放行")
    void systemConversation_selfAllowed() {
        assertThat(com.cartethyia.easyorange.message.domain.aggregate.Message.isConversationParticipant(
                        "conv_1_system", "1"))
                .isTrue();
        assertThat(guard.preSend(subscribe("/queue/chat/conv_1_system", user("1")), null))
                .isNotNull();
    }
}
