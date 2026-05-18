package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.framework.repository.BaseRepository;
import com.cartethyia.easyorange.message.entity.MessageSubscription;
import com.cartethyia.easyorange.message.domain.repository.MessageSubscriptionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisMessageSubscriptionRepository extends BaseRepository<MessageSubscriptionMapper, MessageSubscription> implements MessageSubscriptionRepository {

    public MybatisMessageSubscriptionRepository(MessageSubscriptionMapper mapper) {
        super(mapper);
    }

    @Override
    public List<MessageSubscription> findByUserId(Long userId) {
        return lambdaQuery()
                .eq(MessageSubscription::getUserId, userId)
                .list();
    }

    @Override
    public MessageSubscription findByUserIdAndTypeAndChannel(Long userId, String messageType, String pushChannel) {
        return lambdaQuery()
                .eq(MessageSubscription::getUserId, userId)
                .eq(MessageSubscription::getMessageType, messageType)
                .eq(MessageSubscription::getPushChannel, pushChannel)
                .one();
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
        Long count = lambdaQuery()
                .eq(MessageSubscription::getUserId, userId)
                .eq(MessageSubscription::getMessageType, messageType)
                .eq(MessageSubscription::getPushChannel, pushChannel)
                .eq(MessageSubscription::getEnabled, true)
                .count();
        return count > 0;
    }
}