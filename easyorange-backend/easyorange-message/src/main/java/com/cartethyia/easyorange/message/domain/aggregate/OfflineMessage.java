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
public class OfflineMessage {

    private final String id;
    private final String userId;
    private final String messageId;
    private final String pushChannel;
    private final Integer pushStatus;
    private final Integer retryCount;
    private final Integer maxRetryCount;

    private OfflineMessage(String id, String userId, String messageId, String pushChannel,
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

    public String id() { return id; }
    public String userId() { return userId; }
    public String messageId() { return messageId; }
    public String pushChannel() { return pushChannel; }
    public Integer pushStatus() { return pushStatus; }
    public Integer retryCount() { return retryCount; }
    public Integer maxRetryCount() { return maxRetryCount; }

    // ==================== Factory ====================

    /**
     * 创建离线消息（默认 PENDING 状态）
     */
    public static OfflineMessage create(String userId, String messageId, String pushChannel) {
        return new OfflineMessage(
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
    public static OfflineMessage fromRaw(String id, String userId, String messageId,
                                                    String pushChannel, Integer pushStatus,
                                                    Integer retryCount, Integer maxRetryCount) {
        return new OfflineMessage(id, userId, messageId, pushChannel,
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
    public OfflineMessage markAsPushed() {
        return new OfflineMessage(this.id, this.userId, this.messageId,
                this.pushChannel, MessageConstant.PUSH_STATUS_PUSHED,
                this.retryCount, this.maxRetryCount);
    }

    /**
     * 标记为推送失败
     */
    public OfflineMessage markAsFailed() {
        return new OfflineMessage(this.id, this.userId, this.messageId,
                this.pushChannel, MessageConstant.PUSH_STATUS_FAILED,
                this.retryCount, this.maxRetryCount);
    }

    /**
     * 增加重试次数
     */
    public OfflineMessage incrementRetry() {
        return new OfflineMessage(this.id, this.userId, this.messageId,
                this.pushChannel, this.pushStatus,
                this.retryCount + 1, this.maxRetryCount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OfflineMessage other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "OfflineMessage{id=" + id + ", userId=" + userId + ", pushStatus=" + pushStatus + ", retryCount=" + retryCount + "}";
    }
}
