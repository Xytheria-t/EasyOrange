package com.cartethyia.easyorange.message.domain.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.message.entity.Message;
import com.cartethyia.easyorange.message.enums.ReadStatus;
import com.cartethyia.easyorange.message.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class MybatisMessageRepository implements MessageRepository {

    private final MessageMapper messageMapper;

    @Override
    public Optional<Message> findById(Long id) {
        Message message = messageMapper.selectById(id);
        return Optional.ofNullable(message);
    }

    @Override
    public List<Message> findByReceiverId(Long receiverId, int limit) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, receiverId)
                .orderByDesc(Message::getCreateTime)
                .last("LIMIT " + limit);
        return messageMapper.selectList(wrapper);
    }

    @Override
    public List<Message> findByReceiverIdAndReadStatus(Long receiverId, ReadStatus readStatus, int limit) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, receiverId)
                .eq(Message::getIsRead, readStatus.getCode())
                .orderByDesc(Message::getCreateTime)
                .last("LIMIT " + limit);
        return messageMapper.selectList(wrapper);
    }

    @Override
    public long countUnreadByReceiverId(Long receiverId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, receiverId)
                .eq(Message::getIsRead, ReadStatus.UNREAD.getCode());
        return messageMapper.selectCount(wrapper);
    }

    @Override
    public void save(Message message) {
        messageMapper.insert(message);
    }

    @Override
    public void update(Message message) {
        messageMapper.updateById(message);
    }

    @Override
    public void delete(Long id) {
        messageMapper.deleteById(id);
    }
}
