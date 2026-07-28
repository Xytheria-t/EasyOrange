package com.cartethyia.easyorange.message.service;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;

import java.util.List;

public interface OfflineMessageService {

    void saveOfflineMessage(String userId, String messageId, String pushChannel);

    List<OfflineMessage> getPendingMessages(String userId);

    void markAsPushed(String offlineMessageId);

    void markAsFailed(String offlineMessageId);

    void incrementRetryCount(String offlineMessageId);
}
