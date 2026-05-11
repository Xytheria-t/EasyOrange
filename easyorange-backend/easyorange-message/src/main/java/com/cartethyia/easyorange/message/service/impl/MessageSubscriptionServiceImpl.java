package com.cartethyia.easyorange.message.service.impl;

import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.domain.repository.MessageSubscriptionRepository;
import com.cartethyia.easyorange.message.dto.request.SubscriptionRequest;
import com.cartethyia.easyorange.message.dto.vo.MessageSubscriptionVO;
import com.cartethyia.easyorange.message.entity.MessageSubscription;
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
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return messageSubscriptionRepository.findByUserId(userId)
                .stream()
                .map(sub -> MessageSubscriptionVO.builder()
                        .id(sub.getId())
                        .messageType(sub.getMessageType())
                        .pushChannel(sub.getPushChannel())
                        .enabled(sub.getEnabled())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubscription(SubscriptionRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        MessageSubscription existing = messageSubscriptionRepository.findByUserIdAndTypeAndChannel(
                userId, request.getMessageType(), request.getPushChannel());

        if (existing != null) {
            if (request.getEnabled()) {
                existing.enable();
            } else {
                existing.disable();
            }
            messageSubscriptionRepository.update(existing);
        } else {
            MessageSubscription subscription = MessageSubscription.create(
                    userId, request.getMessageType(), request.getPushChannel(), request.getEnabled());
            messageSubscriptionRepository.save(subscription);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSubscribed(Long userId, String messageType, String pushChannel) {
        return messageSubscriptionRepository.existsEnabled(userId, messageType, pushChannel);
    }
}
