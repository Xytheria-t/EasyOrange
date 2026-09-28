package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import com.cartethyia.easyorange.message.domain.enums.PushStatus;
import java.util.List;

/**
 * 领域聚合 ↔ 数据对象的手写转换（静态工具类，无 Spring Bean）。
 * <p>
 * 取舍：不用 MapStruct——这里每个方法本就是手写 default 实现，注解处理器无可生成，
 * 只会多编译出一个空的 {@code MessageDataMapperImpl} Bean 并让注入点显得有意义。
 * <p>
 * 边界：TINYINT 列（type / push_status）的 String code ↔ Integer 转换集中在这里，
 * 领域侧只见枚举；createTime / updateTime 由 MyBatis-Plus 填充，转换时按实体回读。
 */
public final class MessageDataMapper {

    private MessageDataMapper() {}

    // ── Message ──

    public static MessageDO toEntity(Message aggregate) {
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
                .conversationId(aggregate.conversationId())
                .msgStatus(aggregate.msgStatus())
                .recalledAt(aggregate.recalledAt())
                .build();
    }

    public static Message toAggregate(MessageDO entity) {
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
                entity.getConversationId(),
                entity.getMsgStatus(),
                entity.getRecalledAt(),
                entity.getCreateTime());
    }

    public static List<Message> toAggregateList(List<MessageDO> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(MessageDataMapper::toAggregate).toList();
    }

    // ── OfflineMessage ──

    public static OfflineMessageDO toEntity(OfflineMessage aggregate) {
        if (aggregate == null) {
            return null;
        }
        return OfflineMessageDO.builder()
                .id(aggregate.id())
                .userId(aggregate.userId())
                .messageId(aggregate.messageId())
                .pushChannel(aggregate.pushChannel())
                .pushStatus(pushStatusCode(aggregate.pushStatus()))
                .build();
    }

    public static OfflineMessage toAggregate(OfflineMessageDO entity) {
        if (entity == null) {
            return null;
        }
        return OfflineMessage.fromRaw(
                entity.getId(),
                entity.getUserId(),
                entity.getMessageId(),
                entity.getPushChannel(),
                toPushStatus(entity.getPushStatus()));
    }

    public static List<OfflineMessage> toOfflineAggregateList(List<OfflineMessageDO> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(MessageDataMapper::toAggregate).toList();
    }

    // ── Enum ↔ TINYINT 转换（DB 列存 int，domain 用枚举） ──

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
