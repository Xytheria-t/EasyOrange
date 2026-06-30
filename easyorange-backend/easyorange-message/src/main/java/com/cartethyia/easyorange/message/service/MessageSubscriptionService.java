package com.cartethyia.easyorange.message.service;

import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.SubscriptionRequest;
import com.cartethyia.easyorange.message.application.query.dto.MessageSubscriptionVO;

import java.util.List;

public interface MessageSubscriptionService {

    List<MessageSubscriptionVO> getMySubscriptions();

    void updateSubscription(SubscriptionRequest request);

    boolean isSubscribed(String userId, String messageType, String pushChannel);
}
