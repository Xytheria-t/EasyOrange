package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageDO;
import com.cartethyia.easyorange.message.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.repository.MessageRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisMessageRepository extends BaseRepository<MessageMapper, MessageDO> implements MessageRepository {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final MessageDataMapper messageDataMapper;

    public MybatisMessageRepository(MessageMapper messageMapper, MessageDataMapper messageDataMapper) {
        super(messageMapper);
        this.messageDataMapper = messageDataMapper;
    }

    @Override
    public Optional<MessageAggregate> findById(String id) {
        MessageDO entity = mapper.selectById(id);
        return Optional.ofNullable(messageDataMapper.toAggregate(entity));
    }

    @Override
    public List<MessageAggregate> findByReceiverId(String receiverId, int limit) {
        return messageDataMapper.toAggregateList(
                lambdaQuery()
                        .eq(MessageDO::getReceiverId, receiverId)
                        .orderByDesc(MessageDO::getCreateTime)
                        .last("LIMIT " + limit)
                        .list()
        );
    }

    @Override
    public List<MessageAggregate> findByReceiverIdAndReadStatus(String receiverId, ReadStatus readStatus, int limit) {
        return messageDataMapper.toAggregateList(
                lambdaQuery()
                        .eq(MessageDO::getReceiverId, receiverId)
                        .eq(MessageDO::getIsRead, readStatus)
                        .orderByDesc(MessageDO::getCreateTime)
                        .last("LIMIT " + limit)
                        .list()
            );
    }

    @Override
    public long countUnreadByReceiverId(String receiverId) {
        return lambdaQuery()
                .eq(MessageDO::getReceiverId, receiverId)
                .eq(MessageDO::getIsRead, ReadStatus.UNREAD)
                .count();
    }

    @Override
    public MessageAggregate save(MessageAggregate message) {
        MessageDO entity = messageDataMapper.toEntity(message);
        mapper.insert(entity);
        return messageDataMapper.toAggregate(entity);
    }

    @Override
    public void update(MessageAggregate message) {
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
