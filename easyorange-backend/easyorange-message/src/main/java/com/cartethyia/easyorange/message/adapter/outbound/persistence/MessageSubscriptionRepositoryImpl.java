package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.message.domain.aggregate.MessageSubscription;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageSubscriptionDO;
import com.cartethyia.easyorange.message.domain.repository.MessageSubscriptionRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Primary
@Repository
public class MessageSubscriptionRepositoryImpl extends BaseRepository<MessageSubscriptionMapper, MessageSubscriptionDO> implements MessageSubscriptionRepository {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final MessageDataMapper messageDataMapper;

    public MessageSubscriptionRepositoryImpl(MessageSubscriptionMapper mapper, MessageDataMapper messageDataMapper) {
        super(mapper);
        this.messageDataMapper = messageDataMapper;
    }

    @Override
    public List<MessageSubscription> findByUserId(String userId) {
        return messageDataMapper.toSubscriptionAggregateList(
                lambdaQuery()
                        .eq(MessageSubscriptionDO::getUserId, userId)
                        .list()
        );
    }

    @Override
    public MessageSubscription findByUserIdAndTypeAndChannel(String userId, String messageType, String pushChannel) {
        MessageSubscriptionDO entity = lambdaQuery()
                .eq(MessageSubscriptionDO::getUserId, userId)
                .eq(MessageSubscriptionDO::getMessageType, messageType)
                .eq(MessageSubscriptionDO::getPushChannel, pushChannel)
                .one();
        return messageDataMapper.toAggregate(entity);
    }

    @Override
    public MessageSubscription save(MessageSubscription subscription) {
        MessageSubscriptionDO entity = messageDataMapper.toEntity(subscription);
        mapper.insert(entity);
        return messageDataMapper.toAggregate(entity);
    }

    @Override
    public void update(MessageSubscription subscription) {
        mapper.updateById(messageDataMapper.toEntity(subscription));
    }

    @Override
    public boolean existsEnabled(String userId, String messageType, String pushChannel) {
        Long count = lambdaQuery()
                .eq(MessageSubscriptionDO::getUserId, userId)
                .eq(MessageSubscriptionDO::getMessageType, messageType)
                .eq(MessageSubscriptionDO::getPushChannel, pushChannel)
                .eq(MessageSubscriptionDO::getEnabled, true)
                .count();
        return count > 0;
    }
}
