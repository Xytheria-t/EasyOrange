package com.cartethyia.easyorange.message.domain.aggregate;

import com.cartethyia.easyorange.message.constant.MessageConstant;

/**
 * 离线消息聚合根 —— 不可变对象
 * <p>
 * 核心不变量：
 * <ul>
 *   <li>新创建的离线消息状态为 PENDING，重试计数为 0</li>
 *   <li>只有 PENDING 状态的消息可以重试</li>
 *   <li>重试次数不能超过最大重试次数</li>
 * </ul>
 */
public class OfflineMessageAggregate {

    private final Long id;
    private final Long userId;
    private final Long messageId;
    private final String pushChannel;
    private final Integer pushStatus;
    private final Integer retryCount;
    private final Integer maxRetryCount;

    private OfflineMessageAggregate(Long id, Long userId, Long messageId, String pushChannel,
                                     Integer pushStatus, Integer retryCount, Integer maxRetryCount) {
        this.id = id;
        this.userId = userId;
        this.messageId = messageId;
        this.pushChannel = pushChannel;
        this.pushStatus = pushStatus;
        this.retryCount = retryCount;
        this.maxRetryCount = maxRetryCount;
    }

    // ==================== Getters ====================

    public Long id() { return id; }
    public Long userId() { return userId; }
    public Long messageId() { return messageId; }
    public String pushChannel() { return pushChannel; }
    public Integer pushStatus() { return pushStatus; }
    public Integer retryCount() { return retryCount; }
    public Integer maxRetryCount() { return maxRetryCount; }

    // ==================== Factory ====================

    /**
     * 创建离线消息（默认 PENDING 状态）
     */
    public static OfflineMessageAggregate create(Long userId, Long messageId, String pushChannel) {
        return new OfflineMessageAggregate(
                null, userId, messageId, pushChannel,
                MessageConstant.PUSH_STATUS_PENDING,
                MessageConstant.DEFAULT_RETRY_COUNT,
                MessageConstant.DEFAULT_MAX_RETRY_COUNT
        );
    }

    // ==================== Reconstruction ====================

    /**
     * 从持久层原始数据重建聚合根
     */
    public static OfflineMessageAggregate fromRaw(Long id, Long userId, Long messageId,
                                                    String pushChannel, Integer pushStatus,
                                                    Integer retryCount, Integer maxRetryCount) {
        return new OfflineMessageAggregate(id, userId, messageId, pushChannel,
                pushStatus, retryCount, maxRetryCount);
    }

    // ==================== Predicates ====================

    public boolean isPending() {
        return this.pushStatus != null && this.pushStatus == MessageConstant.PUSH_STATUS_PENDING;
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetryCount;
    }

    // ==================== State Transitions ====================

    /**
     * 标记为已推送
     */
    public OfflineMessageAggregate markAsPushed() {
        return new OfflineMessageAggregate(this.id, this.userId, this.messageId,
                this.pushChannel, MessageConstant.PUSH_STATUS_PUSHED,
                this.retryCount, this.maxRetryCount);
    }

    /**
     * 标记为推送失败
     */
    public OfflineMessageAggregate markAsFailed() {
        return new OfflineMessageAggregate(this.id, this.userId, this.messageId,
                this.pushChannel, MessageConstant.PUSH_STATUS_FAILED,
                this.retryCount, this.maxRetryCount);
    }

    /**
     * 增加重试次数
     */
    public OfflineMessageAggregate incrementRetry() {
        return new OfflineMessageAggregate(this.id, this.userId, this.messageId,
                this.pushChannel, this.pushStatus,
                this.retryCount + 1, this.maxRetryCount);
    }
}
