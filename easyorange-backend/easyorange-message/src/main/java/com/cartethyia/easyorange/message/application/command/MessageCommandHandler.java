package com.cartethyia.easyorange.message.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.DistributedRateLimiter;
import com.cartethyia.easyorange.message.application.service.OfflineMessageAppService;
import com.cartethyia.easyorange.message.application.service.SystemNotificationPayload;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.aggregate.Message.MessageRecallResult;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import com.cartethyia.easyorange.message.domain.port.MessageNotifierPort;
import com.cartethyia.easyorange.message.domain.repository.MessageRepository;
import com.cartethyia.easyorange.message.domain.service.SensitiveWordFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageCommandHandler {

    private static final String MESSAGE_RATE_KEY = "eo:rate:message:%s";
    private static final int MAX_MESSAGES_PER_SECOND = 5;

    private final MessageRepository messageRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final OfflineMessageAppService offlineMessageAppService;
    private final DistributedRateLimiter distributedRateLimiter;
    private final SensitiveWordFilterService sensitiveWordFilterService;
    private final MessageNotifierPort messageNotifier;
    private final IdGenerator idGenerator;

    /**
     * 发送消息 —— REST 与 WebSocket 唯一的发送入口，也是限流与敏感词过滤的唯一裁决点。
     *
     * @return 落库后的消息聚合根；WS 侧据此广播与落库一致的帧（客户端原始入参未经过滤，不能直接回显）
     */
    @Transactional(rollbackFor = Exception.class)
    public Message sendMessage(String senderId, SendMessageCommand command) {
        if (!allowSendMessage(senderId)) {
            throw MessageDomainException.of("发送过于频繁，请稍后再试");
        }

        String filteredContent = sensitiveWordFilterService.filter(command.content());
        String filteredTitle = sensitiveWordFilterService.filter(command.title());

        Message saved = messageRepository.save(Message.create(
                idGenerator.generateId(),
                senderId,
                command.receiverId(),
                normalizeType(command.type()),
                filteredTitle,
                filteredContent,
                command.businessId()));

        notifyAfterCommit(saved, false);

        log.info(
                "action=send_message messageId={} senderId={} receiverId={} type={}",
                saved.id(),
                senderId,
                command.receiverId(),
                saved.type());

        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public void sendSystemMessage(SendSystemMessageCommand command) {
        Message saved = messageRepository.save(Message.createSystem(
                idGenerator.generateId(),
                command.receiverId(),
                command.title(),
                command.content(),
                command.businessId()));

        notifyAfterCommit(saved, true);

        log.info("action=send_system_message messageId={} receiverId={}", saved.id(), command.receiverId());
    }

    /**
     * 实时投递注册到 afterCommit —— 在线检查（Redis）、离线兜底、STOMP 推送全是事务外副作用：
     * 事务回滚后推送出去的消息收不回，离线收件箱也会留下指向不存在消息的幻影记录。
     * 推送失败在提交后只告警不打回请求（DB 行已落库，离线兜底才是可靠路径）。
     * 无事务上下文（单元测试 / 非事务调用方）时立即投递。
     */
    private void notifyAfterCommit(Message saved, boolean systemNotification) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deliverRealtime(saved, systemNotification);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deliverRealtime(saved, systemNotification);
            }
        });
    }

    private void deliverRealtime(Message saved, boolean systemNotification) {
        try {
            // 只有系统通知进离线兜底（replayPending 也只补推 SYSTEM）：聊天消息的会话与未读真相源
            // 就在 eo_message，客户端上线自拉——落离线行只会造出永不消费、永不清账的 PENDING 行
            if (systemNotification) {
                boolean online = messageNotifier.isUserOnline(saved.receiverId());
                offlineMessageAppService.storeIfOffline(saved.receiverId(), saved.id(), "websocket", online);
                if (online) {
                    messageNotifier.sendNotification(saved.receiverId(), SystemNotificationPayload.toMap(saved));
                }
            }
        } catch (Exception e) {
            log.warn(
                    "action=message_realtime_delivery_failed messageId={} receiverId={}",
                    saved.id(),
                    saved.receiverId(),
                    e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(String userId, MarkAsReadCommand command) {
        Message aggregate = messageRepository
                .findById(command.messageId())
                .orElseThrow(() -> MessageDomainException.notFound(command.messageId()));

        // 归属校验由聚合根 read(userId) 承担（同一不变量的唯一实现）
        if (aggregate.isUnread()) {
            messageRepository.update(aggregate.read(userId));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAsReadBatch(String userId, MarkAsReadBatchCommand command) {
        var messageIds = command.messageIds();
        if (messageIds == null || messageIds.isEmpty()) {
            // 空列表 = 无可标记：no-op 成功（前端竞态下清空入参不该被误伤成 400）
            log.debug("action=mark_batch_read_skip reason=empty userId={}", userId);
            return;
        }
        BizRequire.requireTrue(!messageIds.contains(null), "消息ID不能为null");
        // 上限防 IN 子句无限膨胀（一次请求打爆 SQL / 锁表）；50 与批量审核上限同档
        BizRequire.requireTrue(messageIds.size() <= 50, "单次最多标记 50 条消息");

        // 谓词（属于该接收者 + 仍为未读）下推为一条批量 UPDATE，避免逐条读+写 2N 次往返；
        // 非本人 / 不存在 / 已读的 ID 由 SQL 谓词静默跳过，与原逐条语义等价
        messageRepository.markAsReadByIds(userId, messageIds);

        log.info("action=mark_batch_read userId={} count={}", userId, messageIds.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAsReadByType(String userId, Integer type) {
        messageRepository.markAsReadByType(userId, type);
        log.info("action=mark_type_read userId={} type={}", userId, type);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recallMessage(String userId, RecallMessageCommand command) {
        Message aggregate = messageRepository
                .findById(command.messageId())
                .orElseThrow(() -> MessageDomainException.notFound(command.messageId()));

        // senderId 为 null 的系统消息在此快速失败，避免事件里带出空会话
        MessageRecallResult recallResult = aggregate.recall(userId);
        messageRepository.update(recallResult.aggregate());
        domainEventPublisher.publish(recallResult.event());

        log.info("action=recall_message messageId={} userId={}", command.messageId(), userId);
    }

    /**
     * 消息发送限流（Redisson RRateLimiter 令牌桶，5 条/秒/用户）— Redis 不可用时 fail-open 放行，
     * 与框架 RateLimitFilter / AiRateLimitInterceptor 的降级策略一致。
     */
    private boolean allowSendMessage(String userId) {
        try {
            return distributedRateLimiter.tryAcquire(MESSAGE_RATE_KEY.formatted(userId), MAX_MESSAGES_PER_SECOND, 1);
        } catch (Exception e) {
            log.warn("action=rate_limit_fallback userId={}", userId, e);
            return true;
        }
    }

    /**
     * 入参 type 归一化：null 或非法 MessageType code 一律视为聊天消息（CHAT=2）。
     * <p>
     * REST 发送（ProductDetailPage 联系卖家）不带 type、WS 前端发送 type:0，二者都应在
     * 边界归一化为 CHAT，避免 eo_message.type 落入无效值（0）导致分类/未读统计失准。
     */
    private static MessageType normalizeType(Integer type) {
        if (type == null) {
            return MessageType.CHAT;
        }
        try {
            return MessageType.fromCode(String.valueOf(type));
        } catch (IllegalArgumentException e) {
            return MessageType.CHAT;
        }
    }
}
