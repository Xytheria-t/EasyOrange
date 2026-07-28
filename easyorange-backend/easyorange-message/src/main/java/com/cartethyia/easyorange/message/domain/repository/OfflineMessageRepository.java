package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;

import java.util.List;

public interface OfflineMessageRepository {

    OfflineMessage save(OfflineMessage message);

    List<OfflineMessage> findPendingByUserId(String userId);

    void markAsPushed(String offlineMessageId);

    void markAsFailed(String offlineMessageId);

    void incrementRetryCount(String offlineMessageId);
}
