package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.domain.aggregate.MessageSubscriptionAggregate;

import java.util.List;

public interface MessageSubscriptionRepository {

    List<MessageSubscriptionAggregate> findByUserId(String userId);

    MessageSubscriptionAggregate findByUserIdAndTypeAndChannel(String userId, String messageType, String pushChannel);

    MessageSubscriptionAggregate save(MessageSubscriptionAggregate subscription);

    void update(MessageSubscriptionAggregate subscription);

    boolean existsEnabled(String userId, String messageType, String pushChannel);
}
