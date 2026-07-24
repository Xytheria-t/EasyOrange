package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate;
import com.cartethyia.easyorange.message.domain.aggregate.MessageSubscriptionAggregate;
import com.cartethyia.easyorange.message.domain.aggregate.MessageTemplateAggregate;
import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessageAggregate;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageDO;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageSubscriptionDO;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageTemplateDO;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.OfflineMessageDO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MessageDataMapper {

    // ==================== Message ====================

    default MessageDO toEntity(MessageAggregate aggregate) {
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

    default MessageAggregate toAggregate(MessageDO entity) {
        if (entity == null) {
            return null;
        }
        return MessageAggregate.fromRaw(
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

    default List<MessageAggregate> toAggregateList(List<MessageDO> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toAggregate).toList();
    }

    // ==================== OfflineMessage ====================

    default OfflineMessageDO toEntity(OfflineMessageAggregate aggregate) {
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

    default OfflineMessageAggregate toAggregate(OfflineMessageDO entity) {
        if (entity == null) {
            return null;
        }
        return OfflineMessageAggregate.fromRaw(
                entity.getId(),
                entity.getUserId(),
                entity.getMessageId(),
                entity.getPushChannel(),
                entity.getPushStatus(),
                entity.getRetryCount(),
                entity.getMaxRetryCount()
        );
    }

    default List<OfflineMessageAggregate> toOfflineAggregateList(List<OfflineMessageDO> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toAggregate).toList();
    }

    // ==================== MessageSubscription ====================

    default MessageSubscriptionDO toEntity(MessageSubscriptionAggregate aggregate) {
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

    default MessageSubscriptionAggregate toAggregate(MessageSubscriptionDO entity) {
        if (entity == null) {
            return null;
        }
        return MessageSubscriptionAggregate.fromRaw(
                entity.getId(),
                entity.getUserId(),
                entity.getMessageType(),
                entity.getPushChannel(),
                entity.getEnabled()
        );
    }

    default List<MessageSubscriptionAggregate> toSubscriptionAggregateList(List<MessageSubscriptionDO> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toAggregate).toList();
    }

    // ==================== MessageTemplate ====================

    default MessageTemplateDO toEntity(MessageTemplateAggregate aggregate) {
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

    default MessageTemplateAggregate toAggregate(MessageTemplateDO entity) {
        if (entity == null) {
            return null;
        }
        return MessageTemplateAggregate.fromRaw(
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

    default List<MessageTemplateAggregate> toTemplateAggregateList(List<MessageTemplateDO> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toAggregate).toList();
    }
}
