package com.cartethyia.easyorange.message.service;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessageAggregate;

import java.util.List;

public interface OfflineMessageService {

    void saveOfflineMessage(String userId, String messageId, String pushChannel);

    List<OfflineMessageAggregate> getPendingMessages(String userId);

    void markAsPushed(String offlineMessageId);

    void markAsFailed(String offlineMessageId);

    void incrementRetryCount(String offlineMessageId);
}
