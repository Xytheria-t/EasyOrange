package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.repository.MessageRepository;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MessageRepositoryImpl extends BaseRepository<MessageMapper, MessageDO> implements MessageRepository {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final MessageDataMapper messageDataMapper;

    public MessageRepositoryImpl(MessageMapper messageMapper, MessageDataMapper messageDataMapper) {
        super(messageMapper);
        this.messageDataMapper = messageDataMapper;
    }

    @Override
    public Optional<Message> findById(String id) {
        MessageDO entity = mapper.selectById(id);
        return Optional.ofNullable(messageDataMapper.toAggregate(entity));
    }

    @Override
    public Message save(Message message) {
        MessageDO entity = messageDataMapper.toEntity(message);
        mapper.insert(entity);
        return messageDataMapper.toAggregate(entity);
    }

    @Override
    public void update(Message message) {
        mapper.updateById(messageDataMapper.toEntity(message));
    }

    @Override
    public void delete(String id) {
        mapper.deleteById(id);
    }

    @Override
    public void markAllAsRead(String receiverId) {
        lambdaUpdate()
                .eq(MessageDO::getReceiverId, receiverId)
                .eq(MessageDO::getIsRead, ReadStatus.UNREAD)
                .set(MessageDO::getIsRead, ReadStatus.READ)
                .update();
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
}
