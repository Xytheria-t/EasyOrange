package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import java.security.Principal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * STOMP 入站订阅守卫 —— SimpleBroker 的目的地是全局命名空间，握手认证只挡住了「未登录」，
 * 挡不住「已登录但订别人的会话」：conversationId 可由两个 userId 推导（conv_+双方 ID），
 * userId 又可能经商品/订单接口泄露，不拦 SUBSCRIBE 等于把他人聊天内容读给任何已认证用户。
 * <p>
 * 白名单 + 参与者校验：用户目的地（/user/queue/*）由 broker 按会话自绑、天然自 scope，直接放行；
 * 会话目的地（/queue/chat/会话ID、/topic/chat/会话ID/typing 与 recall）必须命中 conv_ 前缀且本人是参与者；
 * 其余一律拒（fail-closed），拒绝以 ERROR 帧回给客户端，不做静默吞订。
 */
@Slf4j
@Component
public class StompSubscribeGuard implements ChannelInterceptor {

    private static final String CHAT_QUEUE_PREFIX = "/queue/chat/";
    private static final String CHAT_TOPIC_PREFIX = "/topic/chat/";
    private static final String TYPING_SUFFIX = "/typing";
    private static final String RECALL_SUFFIX = "/recall";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        Principal user = accessor.getUser();
        if (user == null || user.getName() == null) {
            throw new MessageDeliveryException(message, "未认证的用户");
        }
        String destination = accessor.getDestination();
        // 用户目的地由 broker 自动绑定当前会话（/user/{name}/queue/*），他人订不到
        if (destination == null || destination.startsWith("/user/")) {
            return message;
        }

        String conversationId = extractConversationId(destination);
        if (!com.cartethyia.easyorange.message.domain.aggregate.Message.isConversationParticipant(
                conversationId, user.getName())) {
            log.warn("action=stomp_subscribe_rejected destination={} user={}", destination, user.getName());
            throw new MessageDeliveryException(message, "无权订阅该会话");
        }
        return message;
    }

    /** 从白名单目的地中剥出会话 ID；非白名单目的地返回 null，交给参与者判定统一拒绝。 */
    private static String extractConversationId(String destination) {
        if (destination.startsWith(CHAT_QUEUE_PREFIX)) {
            return destination.substring(CHAT_QUEUE_PREFIX.length());
        }
        if (destination.startsWith(CHAT_TOPIC_PREFIX)
                && (destination.endsWith(TYPING_SUFFIX) || destination.endsWith(RECALL_SUFFIX))) {
            return destination.substring(CHAT_TOPIC_PREFIX.length(), destination.lastIndexOf('/'));
        }
        return null;
    }
}
