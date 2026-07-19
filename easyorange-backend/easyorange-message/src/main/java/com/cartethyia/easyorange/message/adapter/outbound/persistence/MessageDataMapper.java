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
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MessageDataMapper {

    // ==================== Message ====================

    @Mapping(target = "createTime", ignore = true)
    MessageDO toEntity(MessageAggregate aggregate);

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

    OfflineMessageDO toEntity(OfflineMessageAggregate aggregate);

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

    MessageSubscriptionDO toEntity(MessageSubscriptionAggregate aggregate);

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

    MessageTemplateDO toEntity(MessageTemplateAggregate aggregate);

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
