package com.cartethyia.easyorange.message.websocket;

import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.WsMessage;
import com.cartethyia.easyorange.message.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;

    public void sendMessage(Long userId, WsMessage message) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/message",
                    message
            );
        } catch (Exception e) {
            log.warn("action=send_websocket_message_failed userId={}", userId, e);
        }
    }

    public void sendNotification(Long userId, Object notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notification",
                    notification
            );
        } catch (Exception e) {
            log.warn("action=send_websocket_notification_failed userId={}", userId, e);
        }
    }

    public void broadcast(String destination, Object message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.warn("action=broadcast_websocket_message_failed destination={}", destination, e);
        }
    }

    public boolean isUserOnline(Long userId) {
        return userRegistry.getUser(userId.toString()) != null;
    }

    /**
     * 通知买家出价已被接受
     */
    public void notifyOfferAccepted(Long userId, Long productId, BigDecimal acceptedPrice) {
        WsMessage message = WsMessage.builder()
                .type(MessageType.OFFER_ACCEPTED.getCode())
                .businessId(productId)
                .offerPrice(acceptedPrice)
                .content("您的出价 ¥" + acceptedPrice + " 已被接受！")
                .createTime(LocalDateTime.now())
                .build();
        sendMessage(userId, message);
    }

    /**
     * 通知买家出价已被拒绝
     */
    public void notifyOfferRejected(Long userId, Long productId, BigDecimal offerPrice) {
        WsMessage message = WsMessage.builder()
                .type(MessageType.OFFER_REJECTED.getCode())
                .businessId(productId)
                .offerPrice(offerPrice)
                .content("您的出价 ¥" + offerPrice + " 未被接受。")
                .createTime(LocalDateTime.now())
                .build();
        sendMessage(userId, message);
    }

    /**
     * 通知买家收到还价
     */
    public void notifyCounterOffer(Long userId, Long productId, BigDecimal counterPrice) {
        WsMessage message = WsMessage.builder()
                .type(MessageType.COUNTER_OFFER.getCode())
                .businessId(productId)
                .counterPrice(counterPrice)
                .content("卖家还价 ¥" + counterPrice + "，您接受吗？")
                .createTime(LocalDateTime.now())
                .build();
        sendMessage(userId, message);
    }
}
