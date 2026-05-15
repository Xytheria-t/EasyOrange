package com.cartethyia.easyorange.message.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.domain.event.MessageDeletedEvent;
import com.cartethyia.easyorange.message.domain.event.MessageReadEvent;
import com.cartethyia.easyorange.message.domain.event.MessageRecalledEvent;
import com.cartethyia.easyorange.message.domain.event.MessageSentEvent;
import com.cartethyia.easyorange.message.application.command.RecallMessageCommand;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import com.cartethyia.easyorange.message.domain.exception.MessageNotFoundException;
import com.cartethyia.easyorange.message.domain.repository.MessageRepository;
import com.cartethyia.easyorange.message.domain.service.MessageRoutingService;
import com.cartethyia.easyorange.message.domain.service.OfflineMessageStoreService;
import com.cartethyia.easyorange.message.domain.service.RateLimiterService;
import com.cartethyia.easyorange.message.domain.service.SensitiveWordFilterService;
import com.cartethyia.easyorange.message.entity.Message;
import com.cartethyia.easyorange.message.enums.MessageResultCode;
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
    private final MessageRoutingService routingService;
    private final OfflineMessageStoreService offlineMessageStoreService;
    private final RateLimiterService rateLimiterService;
    private final SensitiveWordFilterService sensitiveWordFilterService;

    @Transactional(rollbackFor = Exception.class)
    public void handle(SendMessageCommand command) {
        Long senderId = SecurityContextUtil.getCurrentUserIdOrThrow();

        if (!rateLimiterService.allowSendMessage(senderId)) {
            throw new MessageDomainException("发送过于频繁，请稍后再试");
        }

        String filteredContent = sensitiveWordFilterService.filter(command.getContent());
        String filteredTitle = sensitiveWordFilterService.filter(command.getTitle());

        Message message = Message.create(
                senderId,
                command.getReceiverId(),
                command.getType(),
                filteredTitle,
                filteredContent,
                command.getBusinessId()
        );

        messageRepository.save(message);

        MessageRoutingService.RouteDecision decision = routingService.decideRoute(message.getReceiverId());
        offlineMessageStoreService.storeIfOffline(
                message.getReceiverId(), message.getId(), "websocket", decision.isOnline());

        MessageSentEvent event = message.send();
        domainEventPublisher.publish(event);

        log.info("action=send_message messageId={} senderId={} receiverId={} type={}",
                message.getId(), senderId, command.getReceiverId(), command.getType());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(SendSystemMessageCommand command) {
        Message message = Message.createSystem(
                command.getReceiverId(),
                command.getTitle(),
                command.getContent()
        );

        messageRepository.save(message);

        MessageRoutingService.RouteDecision decision = routingService.decideRoute(message.getReceiverId());
        offlineMessageStoreService.storeIfOffline(
                message.getReceiverId(), message.getId(), "websocket", decision.isOnline());

        MessageSentEvent event = message.send();
        domainEventPublisher.publish(event);

        log.info("action=send_system_message messageId={} receiverId={}",
                message.getId(), command.getReceiverId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(MarkAsReadCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Message message = messageRepository.findById(command.getMessageId())
                .orElseThrow(() -> new MessageNotFoundException(command.getMessageId()));

        BizRequire.requireTrue(
                message.isOwnedBy(userId),
                MessageResultCode.MESSAGE_NOT_OWNER
        );

        if (message.isUnread()) {
            MessageReadEvent event = message.read(userId);
            messageRepository.update(message);
            if (event != null) {
                domainEventPublisher.publish(event);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(MarkAsReadBatchCommand command) {
        BizRequire.notEmpty(command.getMessageIds(), "消息ID列表不能为空");
        BizRequire.noNullElements(command.getMessageIds(), "消息ID不能为null");

        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        for (Long messageId : command.getMessageIds()) {
            try {
                Message message = messageRepository.findById(messageId).orElse(null);
                if (message != null && message.isOwnedBy(userId) && message.isUnread()) {
                    MessageReadEvent event = message.read(userId);
                    messageRepository.update(message);
                    if (event != null) {
                        domainEventPublisher.publish(event);
                    }
                }
            } catch (Exception e) {
                log.warn("action=mark_read_batch_fail messageId={} error={}", messageId, e.getMessage());
            }
        }

        log.info("action=mark_batch_read userId={} count={}", userId, command.getMessageIds().size());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleMarkAllAsRead() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        messageRepository.markAllAsRead(userId);
        log.info("action=mark_all_read userId={}", userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleMarkAsReadByType(Integer type) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        messageRepository.markAsReadByType(userId, type);
        log.info("action=mark_type_read userId={} type={}", userId, type);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(RecallMessageCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Message message = messageRepository.findById(command.getMessageId())
                .orElseThrow(() -> new MessageNotFoundException(command.getMessageId()));

        long minId = Math.min(message.getSenderId(), message.getReceiverId());
        long maxId = Math.max(message.getSenderId(), message.getReceiverId());
        String conversationId = "conv_" + minId + "_" + maxId;
        MessageRecalledEvent event = message.recall(userId, conversationId);
        messageRepository.update(message);
        domainEventPublisher.publish(event);

        log.info("action=recall_message messageId={} userId={}", command.getMessageId(), userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(DeleteMessageCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Message message = messageRepository.findById(command.getMessageId())
                .orElseThrow(() -> new MessageNotFoundException(command.getMessageId()));

        BizRequire.requireTrue(
                message.isOwnedBy(userId),
                MessageResultCode.MESSAGE_NOT_OWNER
        );

        MessageDeletedEvent event = message.delete(userId);
        messageRepository.delete(command.getMessageId());
        domainEventPublisher.publish(event);

        log.info("action=delete_message messageId={} userId={}", command.getMessageId(), userId);
    }
}
