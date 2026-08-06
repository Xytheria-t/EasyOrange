package com.cartethyia.easyorange.message.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.application.service.OfflineMessageStoreService;
import com.cartethyia.easyorange.message.application.service.RateLimiterService;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.aggregate.Message.MessageCreateResult;
import com.cartethyia.easyorange.message.domain.aggregate.Message.MessageReadResult;
import com.cartethyia.easyorange.message.domain.aggregate.Message.MessageRecallResult;
import com.cartethyia.easyorange.message.domain.enums.MessageResultCode;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import com.cartethyia.easyorange.message.domain.event.MessageDeletedEvent;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import com.cartethyia.easyorange.message.domain.exception.MessageNotFoundException;
import com.cartethyia.easyorange.message.domain.port.MessageNotifierPort;
import com.cartethyia.easyorange.message.domain.repository.MessageRepository;
import com.cartethyia.easyorange.message.domain.service.SensitiveWordFilterService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageCommandHandler {

    private final MessageRepository messageRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OfflineMessageStoreService offlineMessageStoreService;
    private final RateLimiterService rateLimiterService;
    private final SensitiveWordFilterService sensitiveWordFilterService;
    private final MessageNotifierPort messageNotifier;

    @Transactional(rollbackFor = Exception.class)
    public void handle(SendMessageCommand command) {
        String senderId = SecurityContextUtil.getCurrentUserIdOrThrow();

        if (!rateLimiterService.allowSendMessage(senderId)) {
            throw new MessageDomainException("发送过于频繁，请稍后再试");
        }

        String filteredContent = sensitiveWordFilterService.filter(command.content());
        String filteredTitle = sensitiveWordFilterService.filter(command.title());

        MessageCreateResult result = Message.create(
                senderId,
                command.receiverId(),
                normalizeType(command.type()),
                filteredTitle,
                filteredContent,
                command.businessId());

        Message saved = messageRepository.save(result.aggregate());

        boolean online = messageNotifier.isUserOnline(saved.receiverId());
        offlineMessageStoreService.storeIfOffline(saved.receiverId(), saved.id(), "websocket", online);

        log.info(
                "action=send_message messageId={} senderId={} receiverId={} type={}",
                saved.id(),
                senderId,
                command.receiverId(),
                saved.type());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(SendSystemMessageCommand command) {
        MessageCreateResult result =
                Message.createSystem(command.receiverId(), command.title(), command.content(), command.businessId());

        Message saved = messageRepository.save(result.aggregate());

        boolean online = messageNotifier.isUserOnline(saved.receiverId());
        offlineMessageStoreService.storeIfOffline(saved.receiverId(), saved.id(), "websocket", online);

        if (online) {
            messageNotifier.sendNotification(
                    saved.receiverId(),
                    Map.of(
                            "id", saved.id(),
                            "title", saved.title() != null ? saved.title() : "",
                            "content", saved.content() != null ? saved.content() : "",
                            "businessId", saved.businessId() != null ? saved.businessId() : "",
                            "type", saved.type(),
                            "createTime",
                                    saved.createTime() != null
                                            ? saved.createTime().toString()
                                            : ""));
        }

        log.info("action=send_system_message messageId={} receiverId={}", saved.id(), command.receiverId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(MarkAsReadCommand command) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Message aggregate = messageRepository
                .findById(command.messageId())
                .orElseThrow(() -> new MessageNotFoundException(command.messageId()));

        BizRequire.requireTrue(aggregate.isOwnedBy(userId), MessageResultCode.MESSAGE_NOT_OWNER);

        if (aggregate.isUnread()) {
            MessageReadResult readResult = aggregate.read(userId);
            if (readResult != null) {
                messageRepository.update(readResult.aggregate());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(MarkAsReadBatchCommand command) {
        BizRequire.notEmpty(command.messageIds(), "消息ID列表不能为空");
        BizRequire.requireTrue(
                command.messageIds() != null && !command.messageIds().contains(null), "消息ID不能为null");

        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        for (String messageId : command.messageIds()) {
            try {
                Message aggregate = messageRepository.findById(messageId).orElse(null);
                if (aggregate != null && aggregate.isOwnedBy(userId) && aggregate.isUnread()) {
                    MessageReadResult readResult = aggregate.read(userId);
                    if (readResult != null) {
                        messageRepository.update(readResult.aggregate());
                    }
                }
            } catch (Exception e) {
                log.warn("action=mark_read_batch_fail messageId={}", messageId, e);
            }
        }

        log.info(
                "action=mark_batch_read userId={} count={}",
                userId,
                command.messageIds().size());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleMarkAllAsRead() {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        messageRepository.markAllAsRead(userId);
        log.info("action=mark_all_read userId={}", userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleMarkAsReadByType(Integer type) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        messageRepository.markAsReadByType(userId, type);
        log.info("action=mark_type_read userId={} type={}", userId, type);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(RecallMessageCommand command) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Message aggregate = messageRepository
                .findById(command.messageId())
                .orElseThrow(() -> new MessageNotFoundException(command.messageId()));

        // 非发送者（含 senderId 为 null 的系统消息）在构造 conversationId 前快速失败，避免 "conv__"。
        BizRequire.requireTrue(aggregate.isSender(userId), MessageResultCode.MESSAGE_NOT_OWNER);

        String conversationId = buildConversationId(aggregate.senderId(), aggregate.receiverId());
        MessageRecallResult recallResult = aggregate.recall(userId, conversationId);
        messageRepository.update(recallResult.aggregate());
        domainEventPublisher.publish(recallResult.event());

        log.info("action=recall_message messageId={} userId={}", command.messageId(), userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(DeleteMessageCommand command) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Message aggregate = messageRepository
                .findById(command.messageId())
                .orElseThrow(() -> new MessageNotFoundException(command.messageId()));

        BizRequire.requireTrue(aggregate.isOwnedBy(userId), MessageResultCode.MESSAGE_NOT_OWNER);

        MessageDeletedEvent event = aggregate.delete(userId);
        messageRepository.delete(command.messageId());

        log.info("action=delete_message messageId={} userId={}", command.messageId(), userId);
    }

    /**
     * 入参 type 归一化：null 或非法 MessageType code 一律视为聊天消息（CHAT=2）。
     * <p>
     * REST 发送（ProductDetailPage 联系卖家）不带 type、WS 前端发送 type:0，二者都应在
     * 边界归一化为 CHAT，避免 eo_message.type 落入无效值（0）导致分类/未读统计失准。
     */
    private static Integer normalizeType(Integer type) {
        if (type == null) {
            return Integer.valueOf(MessageType.CHAT.getCode());
        }
        try {
            MessageType.fromCode(String.valueOf(type));
            return type;
        } catch (IllegalArgumentException e) {
            return Integer.valueOf(MessageType.CHAT.getCode());
        }
    }

    /** 会话 ID：排序双 ID {@code conv_{min}_{max}}，保证 A→B 与 B→A 一致。 */
    private static String buildConversationId(String senderId, String receiverId) {
        if (senderId == null || receiverId == null) {
            return null;
        }
        return senderId.compareTo(receiverId) < 0
                ? "conv_" + senderId + "_" + receiverId
                : "conv_" + receiverId + "_" + senderId;
    }
}
