package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessageAggregate;

import java.util.List;

public interface OfflineMessageRepository {

    OfflineMessageAggregate save(OfflineMessageAggregate message);

    List<OfflineMessageAggregate> findPendingByUserId(Long userId);

    void markAsPushed(Long offlineMessageId);

    void markAsFailed(Long offlineMessageId);

    void incrementRetryCount(Long offlineMessageId);
}
