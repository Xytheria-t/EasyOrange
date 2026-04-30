package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.entity.OfflineMessage;

import java.util.List;

public interface OfflineMessageRepository {

    OfflineMessage save(OfflineMessage message);

    List<OfflineMessage> findPendingByUserId(Long userId);

    void markAsPushed(Long offlineMessageId);

    void markAsFailed(Long offlineMessageId);

    void incrementRetryCount(Long offlineMessageId);
}
