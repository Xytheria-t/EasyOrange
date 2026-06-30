package com.cartethyia.easyorange.message.service.impl;

import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.domain.aggregate.MessageSubscriptionAggregate;
import com.cartethyia.easyorange.message.domain.repository.MessageSubscriptionRepository;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.SubscriptionRequest;
import com.cartethyia.easyorange.message.application.query.dto.MessageSubscriptionVO;
import com.cartethyia.easyorange.message.service.MessageSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageSubscriptionServiceImpl implements MessageSubscriptionService {

    private final MessageSubscriptionRepository messageSubscriptionRepository;

    @Override
    public List<MessageSubscriptionVO> getMySubscriptions() {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return messageSubscriptionRepository.findByUserId(userId)
                .stream()
                .map(sub -> MessageSubscriptionVO.builder()
                        .id(sub.id())
                        .messageType(sub.messageType())
                        .pushChannel(sub.pushChannel())
                        .enabled(sub.enabled())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubscription(SubscriptionRequest request) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        MessageSubscriptionAggregate existing = messageSubscriptionRepository.findByUserIdAndTypeAndChannel(
                userId, request.getMessageType(), request.getPushChannel());

        if (existing != null) {
            MessageSubscriptionAggregate updated = request.getEnabled()
                    ? existing.enable()
                    : existing.disable();
            messageSubscriptionRepository.update(updated);
        } else {
            MessageSubscriptionAggregate subscription = MessageSubscriptionAggregate.create(
                    userId, request.getMessageType(), request.getPushChannel(), request.getEnabled());
            messageSubscriptionRepository.save(subscription);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSubscribed(String userId, String messageType, String pushChannel) {
        return messageSubscriptionRepository.existsEnabled(userId, messageType, pushChannel);
    }
}
