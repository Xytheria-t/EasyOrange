package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.aggregate.MessageSubscription;
import com.cartethyia.easyorange.message.domain.aggregate.MessageTemplate;
import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageDO;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageSubscriptionDO;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageTemplateDO;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.OfflineMessageDO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MessageDataMapper {

    // ==================== Message ====================

    default MessageDO toEntity(Message aggregate) {
        if (aggregate == null) {
            return null;
        }
        return MessageDO.builder()
            .id(aggregate.id())
            .senderId(aggregate.senderId())
            .receiverId(aggregate.receiverId())
            .type(aggregate.type())
            .title(aggregate.title())
            .content(aggregate.content())
            .isRead(aggregate.isRead())
            .readTime(aggregate.readTime())
            .businessId(aggregate.businessId())
            .msgStatus(aggregate.msgStatus())
            .recalledAt(aggregate.recalledAt())
            .build();
    }

    default Message toAggregate(MessageDO entity) {
        if (entity == null) {
            return null;
        }
        return Message.fromRaw(
                entity.getId(),
                entity.getSenderId(),
                entity.getReceiverId(),
                entity.getType(),
                entity.getTitle(),
                entity.getContent(),
                entity.getIsRead(),
                entity.getReadTime(),
                entity.getBusinessId(),
                entity.getMsgStatus(),
                entity.getRecalledAt(),
                entity.getCreateTime()
        );
    }

    default List<Message> toAggregateList(List<MessageDO> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toAggregate).toList();
    }

    // ==================== OfflineMessage ====================

    default OfflineMessageDO toEntity(OfflineMessage aggregate) {
        if (aggregate == null) {
            return null;
        }
        return OfflineMessageDO.builder()
            .id(aggregate.id())
            .userId(aggregate.userId())
            .messageId(aggregate.messageId())
            .pushChannel(aggregate.pushChannel())
            .pushStatus(aggregate.pushStatus())
            .retryCount(aggregate.retryCount())
            .maxRetryCount(aggregate.maxRetryCount())
            .build();
    }

    default OfflineMessage toAggregate(OfflineMessageDO entity) {
        if (entity == null) {
            return null;
        }
        return OfflineMessage.fromRaw(
                entity.getId(),
                entity.getUserId(),
                entity.getMessageId(),
                entity.getPushChannel(),
                entity.getPushStatus(),
                entity.getRetryCount(),
                entity.getMaxRetryCount()
        );
    }

    default List<OfflineMessage> toOfflineAggregateList(List<OfflineMessageDO> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toAggregate).toList();
    }

    // ==================== MessageSubscription ====================

    default MessageSubscriptionDO toEntity(MessageSubscription aggregate) {
        if (aggregate == null) {
            return null;
        }
        return MessageSubscriptionDO.builder()
            .id(aggregate.id())
            .userId(aggregate.userId())
            .messageType(aggregate.messageType())
            .pushChannel(aggregate.pushChannel())
            .enabled(aggregate.enabled())
            .build();
    }

    default MessageSubscription toAggregate(MessageSubscriptionDO entity) {
        if (entity == null) {
            return null;
        }
        return MessageSubscription.fromRaw(
                entity.getId(),
                entity.getUserId(),
                entity.getMessageType(),
                entity.getPushChannel(),
                entity.getEnabled()
        );
    }

    default List<MessageSubscription> toSubscriptionAggregateList(List<MessageSubscriptionDO> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toAggregate).toList();
    }

    // ==================== MessageTemplate ====================

    default MessageTemplateDO toEntity(MessageTemplate aggregate) {
        if (aggregate == null) {
            return null;
        }
        return MessageTemplateDO.builder()
            .id(aggregate.id())
            .templateCode(aggregate.templateCode())
            .templateName(aggregate.templateName())
            .templateType(aggregate.templateType())
            .title(aggregate.title())
            .content(aggregate.content())
            .variables(aggregate.variables())
            .status(aggregate.status())
            .remark(aggregate.remark())
            .build();
    }

    default MessageTemplate toAggregate(MessageTemplateDO entity) {
        if (entity == null) {
            return null;
        }
        return MessageTemplate.fromRaw(
                entity.getId(),
                entity.getTemplateCode(),
                entity.getTemplateName(),
                entity.getTemplateType(),
                entity.getTitle(),
                entity.getContent(),
                entity.getVariables(),
                entity.getStatus(),
                entity.getRemark()
        );
    }

    default List<MessageTemplate> toTemplateAggregateList(List<MessageTemplateDO> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toAggregate).toList();
    }
}
