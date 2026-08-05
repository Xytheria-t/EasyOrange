package com.cartethyia.easyorange.message.application.service;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;

/**
 * 离线消息存储服务 —— 应用层编排。
 * <p>
 * 判断接收方是否在线，若离线则创建 {@link OfflineMessage} 并持久化，待上线后重推。
 * 这是用例编排（读在线状态 → 决定是否持久化），不含跨聚合的领域规则，故属于
 * application 层而非 domain 层；纯领域服务见 {@code SensitiveWordFilterService}。
 */
public class OfflineMessageStoreService {

    private final OfflineMessageRepository offlineMessageRepository;

    /**
     * @param offlineMessageRepository 离线消息仓储
     */
    public OfflineMessageStoreService(OfflineMessageRepository offlineMessageRepository) {
        this.offlineMessageRepository = offlineMessageRepository;
    }

    /**
     * 接收方离线时，将该消息作为离线消息持久化，待其上线后重推。
     *
     * @param userId      接收方用户 ID
     * @param messageId   待存储的消息 ID
     * @param pushChannel 该消息的推送通道
     * @param isOnline    接收方当前是否在线
     */
    public void storeIfOffline(String userId, String messageId, String pushChannel, boolean isOnline) {
        if (!isOnline) {
            OfflineMessage offlineMessage = OfflineMessage.create(userId, messageId, pushChannel);
            offlineMessageRepository.save(offlineMessage);
        }
    }
}
