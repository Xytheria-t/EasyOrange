package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.domain.aggregate.MessageSubscription;

import java.util.List;

public interface MessageSubscriptionRepository {

    List<MessageSubscription> findByUserId(String userId);

    MessageSubscription findByUserIdAndTypeAndChannel(String userId, String messageType, String pushChannel);

    MessageSubscription save(MessageSubscription subscription);

    void update(MessageSubscription subscription);

    boolean existsEnabled(String userId, String messageType, String pushChannel);
}
