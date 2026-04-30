package com.cartethyia.easyorange.message.domain.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.message.entity.MessageSubscription;
import com.cartethyia.easyorange.message.mapper.MessageSubscriptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MybatisMessageSubscriptionRepository implements MessageSubscriptionRepository {

    private final MessageSubscriptionMapper mapper;

    @Override
    public List<MessageSubscription> findByUserId(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<MessageSubscription>()
                .eq(MessageSubscription::getUserId, userId));
    }

    @Override
    public MessageSubscription findByUserIdAndTypeAndChannel(Long userId, String messageType, String pushChannel) {
        return mapper.selectOne(new LambdaQueryWrapper<MessageSubscription>()
                .eq(MessageSubscription::getUserId, userId)
                .eq(MessageSubscription::getMessageType, messageType)
                .eq(MessageSubscription::getPushChannel, pushChannel));
    }

    @Override
    public MessageSubscription save(MessageSubscription subscription) {
        mapper.insert(subscription);
        return subscription;
    }

    @Override
    public void update(MessageSubscription subscription) {
        mapper.updateById(subscription);
    }

    @Override
    public boolean existsEnabled(Long userId, String messageType, String pushChannel) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<MessageSubscription>()
                .eq(MessageSubscription::getUserId, userId)
                .eq(MessageSubscription::getMessageType, messageType)
                .eq(MessageSubscription::getPushChannel, pushChannel)
                .eq(MessageSubscription::getEnabled, true));
        return count > 0;
    }
}
