package com.cartethyia.easyorange.message.domain.service;

import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
import com.cartethyia.easyorange.message.entity.OfflineMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfflineMessageStoreService {

    private final OfflineMessageRepository offlineMessageRepository;

    public void storeIfOffline(Long userId, Long messageId, String pushChannel, boolean isOnline) {
        if (!isOnline) {
            OfflineMessage offlineMessage = OfflineMessage.create(userId, messageId, pushChannel);
            offlineMessageRepository.save(offlineMessage);
        }
    }
}
