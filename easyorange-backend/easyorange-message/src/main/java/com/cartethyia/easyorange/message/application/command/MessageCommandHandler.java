package com.cartethyia.easyorange.message.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate;
import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate.MessageCreateResult;
import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate.MessageReadResult;
import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate.MessageRecallResult;
import com.cartethyia.easyorange.message.domain.event.MessageDeletedEvent;
import com.cartethyia.easyorange.message.domain.event.MessageRecalledEvent;
import com.cartethyia.easyorange.message.domain.event.MessageSentEvent;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import com.cartethyia.easyorange.message.domain.exception.MessageNotFoundException;
import com.cartethyia.easyorange.message.domain.repository.MessageRepository;
import com.cartethyia.easyorange.message.domain.service.MessageRoutingService;
import com.cartethyia.easyorange.message.domain.service.OfflineMessageStoreService;
import com.cartethyia.easyorange.message.domain.service.RateLimiterService;
import com.cartethyia.easyorange.message.domain.service.SensitiveWordFilterService;
import com.cartethyia.easyorange.message.enums.MessageResultCode;
import com.cartethyia.easyorange.message.websocket.WebSocketNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

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
    private final WebSocketNotifier webSocketNotifier;

    @Transactional(rollbackFor = Exception.class)
    public void handle(SendMessageCommand command) {
        String senderId = SecurityContextUtil.getCurrentUserIdOrThrow();

        if (!rateLimiterService.allowSendMessage(senderId)) {
            throw new MessageDomainException("发送过于频繁，请稍后再试");
        }

        String filteredContent = sensitiveWordFilterService.filter(command.getContent());
        String filteredTitle = sensitiveWordFilterService.filter(command.getTitle());

        MessageCreateResult result = MessageAggregate.create(
                senderId,
                command.getReceiverId(),
                command.getType(),
                filteredTitle,
                filteredContent,
                command.getBusinessId()
        );

        MessageAggregate saved = messageRepository.save(result.aggregate());

        MessageRoutingService.RouteDecision decision = routingService.decideRoute(saved.receiverId());
        offlineMessageStoreService.storeIfOffline(
                saved.receiverId(), saved.id(), "websocket", decision.isOnline());

        MessageSentEvent event = saved.send();
        domainEventPublisher.publish(event);

        log.info("action=send_message messageId={} senderId={} receiverId={} type={}",
                saved.id(), senderId, command.getReceiverId(), command.getType());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(SendSystemMessageCommand command) {
        MessageCreateResult result = MessageAggregate.createSystem(
                command.getReceiverId(),
                command.getTitle(),
                command.getContent(),
                command.getBusinessId()
        );

        MessageAggregate saved = messageRepository.save(result.aggregate());

        MessageRoutingService.RouteDecision decision = routingService.decideRoute(saved.receiverId());
        offlineMessageStoreService.storeIfOffline(
                saved.receiverId(), saved.id(), "websocket", decision.isOnline());

        if (decision.isOnline()) {
            webSocketNotifier.sendNotification(saved.receiverId(), Map.of(
                    "id", saved.id(),
                    "title", saved.title() != null ? saved.title() : "",
                    "content", saved.content() != null ? saved.content() : "",
                    "businessId", saved.businessId() != null ? saved.businessId() : "",
                    "type", saved.type(),
                    "createTime", saved.createTime() != null ? saved.createTime().toString() : ""
            ));
        }

        MessageSentEvent event = saved.send();
        domainEventPublisher.publish(event);

        log.info("action=send_system_message messageId={} receiverId={}",
                saved.id(), command.getReceiverId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(MarkAsReadCommand command) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        MessageAggregate aggregate = messageRepository.findById(command.getMessageId())
                .orElseThrow(() -> new MessageNotFoundException(command.getMessageId()));

        BizRequire.requireTrue(
                aggregate.isOwnedBy(userId),
                MessageResultCode.MESSAGE_NOT_OWNER
        );

        if (aggregate.isUnread()) {
            MessageReadResult readResult = aggregate.read(userId);
            if (readResult != null) {
                messageRepository.update(readResult.aggregate());
                domainEventPublisher.publish(readResult.event());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(MarkAsReadBatchCommand command) {
        BizRequire.notEmpty(command.getMessageIds(), "消息ID列表不能为空");
        BizRequire.requireTrue(command.getMessageIds() != null && !command.getMessageIds().contains(null), "消息ID不能为null");

        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        for (String messageId : command.getMessageIds()) {
            try {
                MessageAggregate aggregate = messageRepository.findById(messageId).orElse(null);
                if (aggregate != null && aggregate.isOwnedBy(userId) && aggregate.isUnread()) {
                    MessageReadResult readResult = aggregate.read(userId);
                    if (readResult != null) {
                        messageRepository.update(readResult.aggregate());
                        domainEventPublisher.publish(readResult.event());
                    }
                }
            } catch (Exception e) {
                log.warn("action=mark_read_batch_fail messageId={}", messageId, e);
            }
        }

        log.info("action=mark_batch_read userId={} count={}", userId, command.getMessageIds().size());
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

        MessageAggregate aggregate = messageRepository.findById(command.getMessageId())
                .orElseThrow(() -> new MessageNotFoundException(command.getMessageId()));

        String minId = aggregate.senderId() != null && aggregate.receiverId() != null
                ? (aggregate.senderId().compareTo(aggregate.receiverId()) < 0 ? aggregate.senderId() : aggregate.receiverId())
                : "";
        String maxId = aggregate.senderId() != null && aggregate.receiverId() != null
                ? (aggregate.senderId().compareTo(aggregate.receiverId()) < 0 ? aggregate.receiverId() : aggregate.senderId())
                : "";
        String conversationId = "conv_" + minId + "_" + maxId;
        MessageRecallResult recallResult = aggregate.recall(userId, conversationId);
        messageRepository.update(recallResult.aggregate());
        domainEventPublisher.publish(recallResult.event());

        log.info("action=recall_message messageId={} userId={}", command.getMessageId(), userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(DeleteMessageCommand command) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        MessageAggregate aggregate = messageRepository.findById(command.getMessageId())
                .orElseThrow(() -> new MessageNotFoundException(command.getMessageId()));

        BizRequire.requireTrue(
                aggregate.isOwnedBy(userId),
                MessageResultCode.MESSAGE_NOT_OWNER
        );

        MessageDeletedEvent event = aggregate.delete(userId);
        messageRepository.delete(command.getMessageId());
        domainEventPublisher.publish(event);

        log.info("action=delete_message messageId={} userId={}", command.getMessageId(), userId);
    }
}
