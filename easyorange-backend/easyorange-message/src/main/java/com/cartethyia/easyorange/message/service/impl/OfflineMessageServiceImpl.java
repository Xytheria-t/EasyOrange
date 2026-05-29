package com.cartethyia.easyorange.message.service.impl;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessageAggregate;
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
    public void saveOfflineMessage(Long userId, Long messageId, String pushChannel) {
        OfflineMessageAggregate offlineMessage = OfflineMessageAggregate.create(userId, messageId, pushChannel);
        offlineMessageRepository.save(offlineMessage);
    }

    @Override
    public List<OfflineMessageAggregate> getPendingMessages(Long userId) {
        return offlineMessageRepository.findPendingByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsPushed(Long offlineMessageId) {
        offlineMessageRepository.markAsPushed(offlineMessageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsFailed(Long offlineMessageId) {
        offlineMessageRepository.markAsFailed(offlineMessageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementRetryCount(Long offlineMessageId) {
        offlineMessageRepository.incrementRetryCount(offlineMessageId);
    }
}
