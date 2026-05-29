package com.cartethyia.easyorange.message.service;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessageAggregate;

import java.util.List;

public interface OfflineMessageService {

    void saveOfflineMessage(Long userId, Long messageId, String pushChannel);

    List<OfflineMessageAggregate> getPendingMessages(Long userId);

    void markAsPushed(Long offlineMessageId);

    void markAsFailed(Long offlineMessageId);

    void incrementRetryCount(Long offlineMessageId);
}
