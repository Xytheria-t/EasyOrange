package com.cartethyia.easyorange.message.domain.service;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessageAggregate;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;

public class OfflineMessageStoreService {

    private final OfflineMessageRepository offlineMessageRepository;

    /**
     * Constructs an offline message store service with the required repository.
     *
     * @param offlineMessageRepository repository for persisting offline messages
     */
    public OfflineMessageStoreService(OfflineMessageRepository offlineMessageRepository) {
        this.offlineMessageRepository = offlineMessageRepository;
    }

    /**
     * Persists the message as an offline message if the user is not currently online.
     *
     * @param userId      the ID of the recipient user
     * @param messageId   the ID of the message to store
     * @param pushChannel the channel through which the message would be pushed
     * @param isOnline    whether the user is currently online
     */
    public void storeIfOffline(Long userId, Long messageId, String pushChannel, boolean isOnline) {
        if (!isOnline) {
            OfflineMessageAggregate offlineMessage = OfflineMessageAggregate.create(userId, messageId, pushChannel);
            offlineMessageRepository.save(offlineMessage);
        }
    }
}
