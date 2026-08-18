package com.cartethyia.easyorange.message.application.service;

import com.cartethyia.easyorange.message.application.port.query.MessageQueryRepository;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import com.cartethyia.easyorange.message.domain.port.MessageNotifierPort;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 离线消息服务 —— 应用层编排：离线存储 + 上线补推。
 * <p>
 * 判离存储：接收方离线时创建 {@link OfflineMessage}（PENDING）持久化；
 * 上线补推：用户 WebSocket 连接建立后，把待推送的离线系统通知推到
 * /queue/notification 并标记 PUSHED（状态迁移唯一归属聚合根）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfflineMessageStoreService {

    private final OfflineMessageRepository offlineMessageRepository;
    private final MessageQueryRepository messageQueryRepository;
    private final MessageNotifierPort messageNotifier;

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

    /**
     * 上线补推：把该用户待推送（PENDING）的离线系统通知推到 /queue/notification 并标记 PUSHED。
     * <p>
     * 只补推系统通知——聊天消息的会话数据已在 eo_message，客户端上线后自行拉取会话列表，
     * 无需补推实时帧；原消息已被归档/删除时跳过（离线行保持 PENDING，不误标成功）。
     */
    public void replayPending(String userId) {
        List<OfflineMessage> pending = offlineMessageRepository.findPendingByUserId(userId);
        if (pending.isEmpty()) {
            return;
        }
        for (OfflineMessage offline : pending) {
            Message message = messageQueryRepository.findById(offline.messageId());
            if (message == null) {
                log.warn(
                        "action=offline_message_target_missing offlineId={} messageId={}",
                        offline.id(),
                        offline.messageId());
                continue;
            }
            if (message.type() != MessageType.SYSTEM) {
                continue;
            }
            messageNotifier.sendNotification(userId, SystemNotificationPayload.toMap(message));
            offlineMessageRepository.save(offline.markAsPushed());
            log.debug(
                    "action=offline_message_replayed userId={} offlineId={} messageId={}",
                    userId,
                    offline.id(),
                    offline.messageId());
        }
    }
}
