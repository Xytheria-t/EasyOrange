package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate;
import com.cartethyia.easyorange.message.domain.aggregate.MessageSubscriptionAggregate;
import com.cartethyia.easyorange.message.domain.aggregate.MessageTemplateAggregate;
import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessageAggregate;
import com.cartethyia.easyorange.message.entity.Message;
import com.cartethyia.easyorange.message.entity.MessageSubscription;
import com.cartethyia.easyorange.message.entity.MessageTemplate;
import com.cartethyia.easyorange.message.entity.OfflineMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MessageDataMapper {

    // ==================== Message ====================

    @Mapping(target = "createTime", ignore = true)
    Message toEntity(MessageAggregate aggregate);

    default MessageAggregate toAggregate(Message entity) {
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

    default List<MessageAggregate> toAggregateList(List<Message> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toAggregate).toList();
    }

    // ==================== OfflineMessage ====================

    OfflineMessage toEntity(OfflineMessageAggregate aggregate);

    default OfflineMessageAggregate toAggregate(OfflineMessage entity) {
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

    default List<OfflineMessageAggregate> toOfflineAggregateList(List<OfflineMessage> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toAggregate).toList();
    }

    // ==================== MessageSubscription ====================

    MessageSubscription toEntity(MessageSubscriptionAggregate aggregate);

    default MessageSubscriptionAggregate toAggregate(MessageSubscription entity) {
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

    default List<MessageSubscriptionAggregate> toSubscriptionAggregateList(List<MessageSubscription> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toAggregate).toList();
    }

    // ==================== MessageTemplate ====================

    MessageTemplate toEntity(MessageTemplateAggregate aggregate);

    default MessageTemplateAggregate toAggregate(MessageTemplate entity) {
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

    default List<MessageTemplateAggregate> toTemplateAggregateList(List<MessageTemplate> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toAggregate).toList();
    }
}
