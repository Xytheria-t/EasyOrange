package com.cartethyia.easyorange.message.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.domain.event.MessageDeletedEvent;
import com.cartethyia.easyorange.message.domain.event.MessageReadEvent;
import com.cartethyia.easyorange.message.domain.event.MessageSentEvent;
import com.cartethyia.easyorange.message.domain.exception.MessageNotFoundException;
import com.cartethyia.easyorange.message.domain.repository.MessageRepository;
import com.cartethyia.easyorange.message.entity.Message;
import com.cartethyia.easyorange.message.enums.MessageResultCode;
import com.cartethyia.easyorange.message.enums.MessageType;
import com.cartethyia.easyorange.message.enums.ReadStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageCommandHandler {

    private final MessageRepository messageRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public void handle(SendMessageCommand command) {
        Long senderId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Message message = Message.builder()
                .senderId(senderId)
                .receiverId(command.getReceiverId())
                .type(command.getType())
                .title(HtmlUtils.htmlEscape(command.getTitle()))
                .content(HtmlUtils.htmlEscape(command.getContent()))
                .isRead(ReadStatus.UNREAD.getCode())
                .businessId(command.getBusinessId())
                .build();

        messageRepository.save(message);

        domainEventPublisher.publish(new MessageSentEvent(
                message.getId(),
                senderId,
                command.getReceiverId(),
                command.getType()
        ));

        log.info("action=send_message messageId={} senderId={} receiverId={} type={}",
                message.getId(), senderId, command.getReceiverId(), command.getType());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(SendSystemMessageCommand command) {
        Message message = Message.builder()
                .senderId(null)
                .receiverId(command.getReceiverId())
                .type(MessageType.SYSTEM.getCode())
                .title(HtmlUtils.htmlEscape(command.getTitle()))
                .content(HtmlUtils.htmlEscape(command.getContent()))
                .isRead(ReadStatus.UNREAD.getCode())
                .build();

        messageRepository.save(message);

        domainEventPublisher.publish(new MessageSentEvent(
                message.getId(),
                null,
                command.getReceiverId(),
                MessageType.SYSTEM.getCode()
        ));

        log.info("action=send_system_message messageId={} receiverId={}",
                message.getId(), command.getReceiverId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(MarkAsReadCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Message message = messageRepository.findById(command.getMessageId())
                .orElseThrow(() -> new MessageNotFoundException(command.getMessageId()));

        BizRequire.isTrue(
                message.getReceiverId().equals(userId),
                MessageResultCode.MESSAGE_NOT_OWNER
        );

        if (ReadStatus.UNREAD.getCode().equals(message.getIsRead())) {
            message.setIsRead(ReadStatus.READ.getCode());
            messageRepository.update(message);
            domainEventPublisher.publish(new MessageReadEvent(command.getMessageId(), userId));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(MarkAsReadBatchCommand command) {
        if (command.getMessageIds() == null || command.getMessageIds().isEmpty()) {
            return;
        }

        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        for (Long messageId : command.getMessageIds()) {
            try {
                Message message = messageRepository.findById(messageId).orElse(null);
                if (message != null && message.getReceiverId().equals(userId)
                        && ReadStatus.UNREAD.getCode().equals(message.getIsRead())) {
                    message.setIsRead(ReadStatus.READ.getCode());
                    messageRepository.update(message);
                    domainEventPublisher.publish(new MessageReadEvent(messageId, userId));
                }
            } catch (Exception e) {
                log.warn("action=mark_read_batch_fail messageId={} error={}", messageId, e.getMessage());
            }
        }

        log.info("action=mark_batch_read userId={} count={}", userId, command.getMessageIds().size());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(DeleteMessageCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Message message = messageRepository.findById(command.getMessageId())
                .orElseThrow(() -> new MessageNotFoundException(command.getMessageId()));

        BizRequire.isTrue(
                message.getReceiverId().equals(userId),
                MessageResultCode.MESSAGE_NOT_OWNER
        );

        messageRepository.delete(command.getMessageId());
        domainEventPublisher.publish(new MessageDeletedEvent(command.getMessageId(), userId));

        log.info("action=delete_message messageId={} userId={}", command.getMessageId(), userId);
    }
}
