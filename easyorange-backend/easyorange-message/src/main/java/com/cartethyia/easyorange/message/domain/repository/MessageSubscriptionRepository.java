package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.entity.MessageSubscription;

import java.util.List;

public interface MessageSubscriptionRepository {

    List<MessageSubscription> findByUserId(Long userId);

    MessageSubscription findByUserIdAndTypeAndChannel(Long userId, String messageType, String pushChannel);

    MessageSubscription save(MessageSubscription subscription);

    void update(MessageSubscription subscription);

    boolean existsEnabled(Long userId, String messageType, String pushChannel);
}
