package com.cartethyia.easyorange.message.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cartethyia.easyorange.message.entity.OfflineMessage;

import java.util.List;

public interface OfflineMessageService extends IService<OfflineMessage> {

    void saveOfflineMessage(Long userId, Long messageId, String pushChannel);

    List<OfflineMessage> getPendingMessages(Long userId);

    void markAsPushed(Long offlineMessageId);

    void markAsFailed(Long offlineMessageId);

    void incrementRetryCount(Long offlineMessageId);
}
