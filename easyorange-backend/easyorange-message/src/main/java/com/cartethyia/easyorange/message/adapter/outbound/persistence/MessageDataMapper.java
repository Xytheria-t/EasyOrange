package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import com.cartethyia.easyorange.message.domain.enums.PushStatus;
import java.util.List;
import org.mapstruct.Mapper;

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
                .type(typeCode(aggregate.type()))
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
                toType(entity.getType()),
                entity.getTitle(),
                entity.getContent(),
                entity.getIsRead(),
                entity.getReadTime(),
                entity.getBusinessId(),
                entity.getMsgStatus(),
                entity.getRecalledAt(),
                entity.getCreateTime());
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
                .pushStatus(pushStatusCode(aggregate.pushStatus()))
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
                toPushStatus(entity.getPushStatus()),
                entity.getRetryCount(),
                entity.getMaxRetryCount());
    }

    default List<OfflineMessage> toOfflineAggregateList(List<OfflineMessageDO> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toAggregate).toList();
    }

    // ==================== Enum ↔ TINYINT 转换（DB 列存 int，domain 用语义枚举） ====================

    private static Integer typeCode(MessageType type) {
        return type == null ? null : Integer.valueOf(type.getCode());
    }

    private static MessageType toType(Integer code) {
        return code == null ? null : MessageType.fromCode(String.valueOf(code));
    }

    private static Integer pushStatusCode(PushStatus status) {
        return status == null ? null : Integer.valueOf(status.getCode());
    }

    private static PushStatus toPushStatus(Integer code) {
        return code == null ? null : PushStatus.fromCode(String.valueOf(code));
    }
}
