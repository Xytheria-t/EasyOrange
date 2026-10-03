package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.WsMessage;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendMessageCommand;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import com.cartethyia.easyorange.message.domain.port.UserInfoPort;
import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {

    /** 技术异常不回显细节，只给发送方一句可行动的提示。 */
    private static final String GENERIC_ERROR = "消息处理失败，请稍后再试";

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageCommandHandler messageCommandHandler;
    private final UserInfoPort userInfoPort;

    @MessageMapping("/chat.send")
    public void handleChatMessage(@Payload WsMessage payload, Principal principal) {
        String userId = requireUserId(principal);

        // WS 入参不过 @Valid（校验注解只在 HTTP 绑定时生效），必填项在此补齐，
        // 否则 null receiverId 会一路走到插入 NOT NULL 列失败，抛的是技术异常而非可回帧的业务错误
        BizRequire.notBlank(payload.getReceiverId(), "接收方不能为空");
        BizRequire.notBlank(payload.getContent(), "消息内容不能为空");

        // 限流唯一裁决点在 MessageCommandHandler（REST 与 WS 共用），此处不再前置扣减，
        // 避免每条 WS 消息被双重计数导致有效限流阈值减半。超限由 @MessageExceptionHandler 映射错误帧。
        SendMessageCommand command = new SendMessageCommand(
                payload.getReceiverId(),
                payload.getType(),
                payload.getTitle() != null ? payload.getTitle() : "",
                payload.getContent(),
                payload.getBusinessId());
        // STOMP 线程 SecurityContextHolder 不可用，显式传主身份
        Message sent = messageCommandHandler.sendMessage(userId, command);

        // 广播落库后的消息而非客户端原始帧：过滤在 command handler 内完成，
        // 回显原始入参等于让前端拿到未脱敏文本（且 id / createTime 均为 null）
        String conversationId = sent.conversationId();
        messagingTemplate.convertAndSend("/queue/chat/" + conversationId, (Object) chatFrame(sent));

        messagingTemplate.convertAndSendToUser(
                sent.receiverId(),
                "/queue/unread-count",
                Map.of(
                        "conversationId",
                        conversationId,
                        "increment",
                        1,
                        "timestamp",
                        Instant.now().toEpochMilli()));

        log.info(
                "action=chat_message_sent conversationId={} senderId={} receiverId={}",
                conversationId,
                userId,
                sent.receiverId());
    }

    /** 消息命令异常（当前为限流）统一映射为发送方错误帧，避免在 STOMP 线程上抛出未捕获异常。 */
    @MessageExceptionHandler(MessageDomainException.class)
    public void handleDomainException(MessageDomainException ex, Principal principal) {
        log.warn(
                "action=chat_send_rejected error={} user={}",
                ex.getMessage(),
                principal != null ? principal.getName() : null);
        sendErrorFrame(principal, "RATE_LIMITED", ex.getMessage());
    }

    /**
     * 兜底：技术异常（DB / 序列化 / 空指针）也映射为同一形状的错误帧。
     * 没有它，STOMP 线程上的未捕获异常只进服务端日志，发送方永远等不到回执。
     * 业务异常回显原 message（校验文案要给用户看），技术异常只给通用文案，细节留日志。
     */
    @MessageExceptionHandler(Exception.class)
    public void handleUnexpectedException(Exception ex, Principal principal) {
        log.error("action=chat_send_failed user={}", principal != null ? principal.getName() : null, ex);
        String message = ex instanceof BusinessException business ? business.getMessage() : GENERIC_ERROR;
        sendErrorFrame(principal, "ERROR", message);
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload WsMessage payload, Principal principal) {
        String userId = requireUserId(principal);

        // typing 是会话广播帧，无归属校验则任何人可向任意会话注入输入状态；
        // 攻击面拒绝无需回帧提示（合法客户端不会命中）
        if (!Message.isConversationParticipant(payload.getConversationId(), userId)) {
            log.warn(
                    "action=typing_indicator_rejected conversationId={} userId={}",
                    payload.getConversationId(),
                    userId);
            return;
        }

        messagingTemplate.convertAndSend("/topic/chat/" + payload.getConversationId() + "/typing", (Object)
                Map.of("userId", userId, "timestamp", Instant.now().toEpochMilli()));

        log.debug("action=typing_indicator conversationId={} userId={}", payload.getConversationId(), userId);
    }

    public void broadcastRecallEvent(String conversationId, String messageId, String operatorId) {
        String recallDest = "/topic/chat/" + conversationId + "/recall";
        Map<String, Object> recallPayload = Map.of(
                "messageId", String.valueOf(messageId),
                "conversationId", conversationId,
                "operatorId", String.valueOf(operatorId),
                "recalledAt", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend(recallDest, (Object) recallPayload);

        log.info(
                "action=recall_broadcast conversationId={} messageId={} operatorId={}",
                conversationId,
                messageId,
                operatorId);
    }

    /** 从握手认证建立的 Principal 取当前用户 ID（STOMP 线程上 SecurityContextHolder 不可用）。 */
    private static String requireUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw MessageDomainException.of("未认证的用户");
        }
        return principal.getName();
    }

    private void sendErrorFrame(Principal principal, String type, String message) {
        if (principal != null && principal.getName() != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(), "/queue/error", Map.of("type", type, "message", message));
        }
    }

    /**
     * 聊天帧字段与 REST 会话详情（{@code ConversationVO}）对齐：前端把回显帧直接当消息对象存进 store，
     * 少一个字段就少一处客户端补默认值。可空字段按 {@code SystemNotificationPayload} 的同款口径兜底；
     * 发送者档案与 {@code ConversationVO} 同源（UserInfoPort），纯实时会话（未刷新）也能取到对方头像。
     */
    private Map<String, Object> chatFrame(Message message) {
        UserInfo sender = message.senderId() == null
                ? null
                : userInfoPort.getUserInfoMap(Set.of(message.senderId())).get(message.senderId());
        Map<String, Object> frame = new HashMap<>();
        frame.put("id", message.id());
        frame.put("senderId", message.senderId());
        frame.put("senderName", sender != null ? sender.username() : null);
        frame.put("senderAvatar", sender != null ? sender.avatar() : null);
        frame.put("receiverId", message.receiverId());
        frame.put("conversationId", message.conversationId());
        frame.put(
                "type",
                message.type() == null ? null : Integer.valueOf(message.type().getCode()));
        frame.put("title", message.title() != null ? message.title() : "");
        frame.put("content", message.content() != null ? message.content() : "");
        frame.put(
                "isRead",
                message.isRead() == null
                        ? null
                        : Integer.valueOf(message.isRead().getCode()));
        frame.put("readTime", message.readTime() != null ? message.readTime().toString() : null);
        frame.put(
                "status",
                message.msgStatus() == null ? null : message.msgStatus().getCode());
        frame.put(
                "createTime",
                message.createTime() != null ? message.createTime().toString() : "");
        return frame;
    }
}
