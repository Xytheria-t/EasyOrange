package com.cartethyia.easyorange.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.message.constant.MessageConstant;
import com.cartethyia.easyorange.message.entity.OfflineMessage;
import com.cartethyia.easyorange.message.mapper.OfflineMessageMapper;
import com.cartethyia.easyorange.message.service.OfflineMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfflineMessageServiceImpl extends ServiceImpl<OfflineMessageMapper, OfflineMessage> implements OfflineMessageService {

    @Override
    public void saveOfflineMessage(Long userId, Long messageId, String pushChannel) {
        OfflineMessage offlineMessage = OfflineMessage.builder()
                .userId(userId)
                .messageId(messageId)
                .pushChannel(pushChannel)
                .pushStatus(MessageConstant.PUSH_STATUS_PENDING)
                .retryCount(MessageConstant.DEFAULT_RETRY_COUNT)
                .maxRetryCount(MessageConstant.DEFAULT_MAX_RETRY_COUNT)
                .build();
        save(offlineMessage);
    }

    @Override
    public List<OfflineMessage> getPendingMessages(Long userId) {
        return list(new LambdaQueryWrapper<OfflineMessage>()
                .eq(OfflineMessage::getUserId, userId)
                .eq(OfflineMessage::getPushStatus, MessageConstant.PUSH_STATUS_PENDING)
                .orderByAsc(OfflineMessage::getCreateTime))
                .stream()
                .filter(msg -> msg.getRetryCount() < msg.getMaxRetryCount())
                .collect(Collectors.toList());
    }

    @Override
    public void markAsPushed(Long offlineMessageId) {
        update(new LambdaUpdateWrapper<OfflineMessage>()
                .eq(OfflineMessage::getId, offlineMessageId)
                .set(OfflineMessage::getPushStatus, MessageConstant.PUSH_STATUS_PUSHED)
                .set(OfflineMessage::getPushTime, LocalDateTime.now()));
    }

    @Override
    public void markAsFailed(Long offlineMessageId) {
        update(new LambdaUpdateWrapper<OfflineMessage>()
                .eq(OfflineMessage::getId, offlineMessageId)
                .set(OfflineMessage::getPushStatus, MessageConstant.PUSH_STATUS_FAILED));
    }

    @Override
    public void incrementRetryCount(Long offlineMessageId) {
        update(new LambdaUpdateWrapper<OfflineMessage>()
                .eq(OfflineMessage::getId, offlineMessageId)
                .setSql("retry_count = retry_count + 1")
                .set(OfflineMessage::getLastRetryTime, LocalDateTime.now()));
    }
}
