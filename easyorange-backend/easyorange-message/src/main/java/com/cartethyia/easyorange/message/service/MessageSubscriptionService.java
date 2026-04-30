package com.cartethyia.easyorange.message.service;

import com.cartethyia.easyorange.message.dto.request.SubscriptionRequest;
import com.cartethyia.easyorange.message.dto.vo.MessageSubscriptionVO;

import java.util.List;

public interface MessageSubscriptionService {

    List<MessageSubscriptionVO> getMySubscriptions();

    void updateSubscription(SubscriptionRequest request);

    boolean isSubscribed(Long userId, String messageType, String pushChannel);
}
