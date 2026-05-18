package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.framework.repository.BaseRepository;
import com.cartethyia.easyorange.message.entity.Message;
import com.cartethyia.easyorange.message.enums.MessageStatus;
import com.cartethyia.easyorange.message.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.repository.MessageRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisMessageRepository extends BaseRepository<MessageMapper, Message> implements MessageRepository {

    public MybatisMessageRepository(MessageMapper messageMapper) {
        super(messageMapper);
    }

    @Override
    public Optional<Message> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public List<Message> findByReceiverId(Long receiverId, int limit) {
        return lambdaQuery()
                .eq(Message::getReceiverId, receiverId)
                .orderByDesc(Message::getCreateTime)
                .last("LIMIT " + limit)
                .list();
    }

    @Override
    public List<Message> findByReceiverIdAndReadStatus(Long receiverId, ReadStatus readStatus, int limit) {
        return lambdaQuery()
                .eq(Message::getReceiverId, receiverId)
                .eq(Message::getIsRead, readStatus.getCode())
                .orderByDesc(Message::getCreateTime)
                .last("LIMIT " + limit)
                .list();
    }

    @Override
    public long countUnreadByReceiverId(Long receiverId) {
        return lambdaQuery()
                .eq(Message::getReceiverId, receiverId)
                .eq(Message::getIsRead, MessageStatus.UNREAD.getCode())
                .count();
    }

    @Override
    public void save(Message message) {
        mapper.insert(message);
    }

    @Override
    public void update(Message message) {
        mapper.updateById(message);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void markAllAsRead(Long receiverId) {
        lambdaUpdate()
                .eq(Message::getReceiverId, receiverId)
                .eq(Message::getIsRead, MessageStatus.UNREAD.getCode())
                .set(Message::getIsRead, MessageStatus.READ.getCode())
                .update();
    }

    @Override
    public void markAsReadByType(Long receiverId, Integer type) {
        lambdaUpdate()
                .eq(Message::getReceiverId, receiverId)
                .eq(Message::getType, type)
                .eq(Message::getIsRead, MessageStatus.UNREAD.getCode())
                .set(Message::getIsRead, MessageStatus.READ.getCode())
                .update();
    }
}