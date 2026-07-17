package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.message.domain.aggregate.MessageSubscriptionAggregate;
import com.cartethyia.easyorange.message.entity.MessageSubscription;
import com.cartethyia.easyorange.message.domain.repository.MessageSubscriptionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisMessageSubscriptionRepository extends BaseRepository<MessageSubscriptionMapper, MessageSubscription> implements MessageSubscriptionRepository {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final MessageDataMapper messageDataMapper;

    public MybatisMessageSubscriptionRepository(MessageSubscriptionMapper mapper, MessageDataMapper messageDataMapper) {
        super(mapper);
        this.messageDataMapper = messageDataMapper;
    }

    @Override
    public List<MessageSubscriptionAggregate> findByUserId(String userId) {
        return messageDataMapper.toSubscriptionAggregateList(
                lambdaQuery()
                        .eq(MessageSubscription::getUserId, userId)
                        .list()
        );
    }

    @Override
    public MessageSubscriptionAggregate findByUserIdAndTypeAndChannel(String userId, String messageType, String pushChannel) {
        MessageSubscription entity = lambdaQuery()
                .eq(MessageSubscription::getUserId, userId)
                .eq(MessageSubscription::getMessageType, messageType)
                .eq(MessageSubscription::getPushChannel, pushChannel)
                .one();
        return messageDataMapper.toAggregate(entity);
    }

    @Override
    public MessageSubscriptionAggregate save(MessageSubscriptionAggregate subscription) {
        MessageSubscription entity = messageDataMapper.toEntity(subscription);
        mapper.insert(entity);
        return messageDataMapper.toAggregate(entity);
    }

    @Override
    public void update(MessageSubscriptionAggregate subscription) {
        mapper.updateById(messageDataMapper.toEntity(subscription));
    }

    @Override
    public boolean existsEnabled(String userId, String messageType, String pushChannel) {
        Long count = lambdaQuery()
                .eq(MessageSubscription::getUserId, userId)
                .eq(MessageSubscription::getMessageType, messageType)
                .eq(MessageSubscription::getPushChannel, pushChannel)
                .eq(MessageSubscription::getEnabled, true)
                .count();
        return count > 0;
    }
}
