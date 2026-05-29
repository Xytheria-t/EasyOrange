package com.cartethyia.easyorange.message.domain.service;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessageAggregate;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;

public class OfflineMessageStoreService {

    private final OfflineMessageRepository offlineMessageRepository;

    public OfflineMessageStoreService(OfflineMessageRepository offlineMessageRepository) {
        this.offlineMessageRepository = offlineMessageRepository;
    }

    public void storeIfOffline(Long userId, Long messageId, String pushChannel, boolean isOnline) {
        if (!isOnline) {
            OfflineMessageAggregate offlineMessage = OfflineMessageAggregate.create(userId, messageId, pushChannel);
            offlineMessageRepository.save(offlineMessage);
        }
    }
}
