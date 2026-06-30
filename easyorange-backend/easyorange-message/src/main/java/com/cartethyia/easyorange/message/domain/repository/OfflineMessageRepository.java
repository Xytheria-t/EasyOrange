package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessageAggregate;

import java.util.List;

public interface OfflineMessageRepository {

    OfflineMessageAggregate save(OfflineMessageAggregate message);

    List<OfflineMessageAggregate> findPendingByUserId(String userId);

    void markAsPushed(String offlineMessageId);

    void markAsFailed(String offlineMessageId);

    void incrementRetryCount(String offlineMessageId);
}
