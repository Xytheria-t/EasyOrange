package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.repository.MessageRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MessageRepositoryImpl extends BaseRepository<MessageMapper, MessageDO> implements MessageRepository {

    public MessageRepositoryImpl(MessageMapper messageMapper) {
        super(messageMapper);
    }

    @Override
    public Optional<Message> findById(String id) {
        MessageDO entity = mapper.selectById(id);
        return Optional.ofNullable(MessageDataMapper.toAggregate(entity));
    }

    @Override
    public Message save(Message message) {
        MessageDO entity = MessageDataMapper.toEntity(message);
        mapper.insert(entity);
        return MessageDataMapper.toAggregate(entity);
    }

    @Override
    public void update(Message message) {
        mapper.updateById(MessageDataMapper.toEntity(message));
    }

    @Override
    public void markAsReadByType(String receiverId, Integer type) {
        lambdaUpdate()
                .eq(MessageDO::getReceiverId, receiverId)
                .eq(MessageDO::getType, type)
                .eq(MessageDO::getIsRead, ReadStatus.UNREAD)
                .set(MessageDO::getIsRead, ReadStatus.READ)
                .update();
    }

    @Override
    public void markAsReadByIds(String receiverId, List<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        lambdaUpdate()
                .eq(MessageDO::getReceiverId, receiverId)
                .in(MessageDO::getId, messageIds)
                .eq(MessageDO::getIsRead, ReadStatus.UNREAD)
                .set(MessageDO::getIsRead, ReadStatus.READ)
                .update();
    }
}
