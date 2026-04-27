package com.cartethyia.easyorange.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.message.dto.request.SubscriptionRequest;
import com.cartethyia.easyorange.message.dto.vo.MessageSubscriptionVO;
import com.cartethyia.easyorange.message.entity.MessageSubscription;
import com.cartethyia.easyorange.message.mapper.MessageSubscriptionMapper;
import com.cartethyia.easyorange.message.service.MessageSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageSubscriptionServiceImpl extends ServiceImpl<MessageSubscriptionMapper, MessageSubscription> implements MessageSubscriptionService {

    @Override
    public List<MessageSubscriptionVO> getMySubscriptions() {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return lambdaQuery()
                .eq(MessageSubscription::getUserId, userId)
                .list()
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

        LambdaQueryWrapper<MessageSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageSubscription::getUserId, userId)
                .eq(MessageSubscription::getMessageType, request.getMessageType())
                .eq(MessageSubscription::getPushChannel, request.getPushChannel());

        MessageSubscription existing = getOne(wrapper);
        if (existing != null) {
            existing.setEnabled(request.getEnabled());
            updateById(existing);
        } else {
            MessageSubscription subscription = MessageSubscription.builder()
                    .userId(userId)
                    .messageType(request.getMessageType())
                    .pushChannel(request.getPushChannel())
                    .enabled(request.getEnabled())
                    .build();
            save(subscription);
        }
    }

    @Override
    public boolean isSubscribed(Long userId, String messageType, String pushChannel) {
        return count(new LambdaQueryWrapper<MessageSubscription>()
                .eq(MessageSubscription::getUserId, userId)
                .eq(MessageSubscription::getMessageType, messageType)
                .eq(MessageSubscription::getPushChannel, pushChannel)
                .eq(MessageSubscription::getEnabled, true)) > 0;
    }
}
