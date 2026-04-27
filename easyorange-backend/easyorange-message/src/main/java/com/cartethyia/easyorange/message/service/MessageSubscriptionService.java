package com.cartethyia.easyorange.message.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cartethyia.easyorange.message.dto.request.SubscriptionRequest;
import com.cartethyia.easyorange.message.dto.vo.MessageSubscriptionVO;
import com.cartethyia.easyorange.message.entity.MessageSubscription;

import java.util.List;

public interface MessageSubscriptionService extends IService<MessageSubscription> {

    List<MessageSubscriptionVO> getMySubscriptions();

    void updateSubscription(SubscriptionRequest request);

    boolean isSubscribed(Long userId, String messageType, String pushChannel);
}
