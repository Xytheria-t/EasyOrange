package com.cartethyia.easyorange.message.service.impl;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
import com.cartethyia.easyorange.message.service.OfflineMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfflineMessageServiceImpl implements OfflineMessageService {

    private final OfflineMessageRepository offlineMessageRepository;

    @Override
    public void saveOfflineMessage(String userId, String messageId, String pushChannel) {
        OfflineMessage offlineMessage = OfflineMessage.create(userId, messageId, pushChannel);
        offlineMessageRepository.save(offlineMessage);
    }

    @Override
    public List<OfflineMessage> getPendingMessages(String userId) {
        return offlineMessageRepository.findPendingByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsPushed(String offlineMessageId) {
        offlineMessageRepository.markAsPushed(offlineMessageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsFailed(String offlineMessageId) {
        offlineMessageRepository.markAsFailed(offlineMessageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementRetryCount(String offlineMessageId) {
        offlineMessageRepository.incrementRetryCount(offlineMessageId);
    }
}
