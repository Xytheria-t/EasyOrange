package com.cartethyia.easyorange.message.domain.aggregate;

import com.cartethyia.easyorange.message.domain.constant.MessageConstant;
import com.cartethyia.easyorange.message.domain.enums.PushStatus;

/**
 * 离线消息聚合根 —— 不可变 record
 * <p>
 * 核心不变量：
 * <ul>
 *   <li>新创建的离线消息状态为 PENDING，重试计数为 0</li>
 *   <li>只有 PENDING 状态的消息可以重试</li>
 *   <li>重试次数不能超过最大重试次数</li>
 * </ul>
 */
public record OfflineMessage(
        String id,
        String userId,
        String messageId,
        String pushChannel,
        PushStatus pushStatus,
        Integer retryCount,
        Integer maxRetryCount) {

    // ==================== Factory ====================

    /**
     * 创建离线消息（默认 PENDING 状态）
     */
    public static OfflineMessage create(String userId, String messageId, String pushChannel) {
        return new OfflineMessage(
                null,
                userId,
                messageId,
                pushChannel,
                PushStatus.PENDING,
                MessageConstant.DEFAULT_RETRY_COUNT,
                MessageConstant.DEFAULT_MAX_RETRY_COUNT);
    }

    // ==================== Reconstruction ====================

    /**
     * 从持久层原始数据重建聚合根
     */
    public static OfflineMessage fromRaw(
            String id,
            String userId,
            String messageId,
            String pushChannel,
            PushStatus pushStatus,
            Integer retryCount,
            Integer maxRetryCount) {
        return new OfflineMessage(id, userId, messageId, pushChannel, pushStatus, retryCount, maxRetryCount);
    }

    // ==================== Predicates ====================

    public boolean isPending() {
        return PushStatus.PENDING == this.pushStatus;
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetryCount;
    }

    // ==================== State Transitions ====================

    /**
     * 标记为已推送
     */
    public OfflineMessage markAsPushed() {
        return new OfflineMessage(
                this.id, this.userId, this.messageId, this.pushChannel, PushStatus.PUSHED, this.retryCount,
                this.maxRetryCount);
    }

    /**
     * 标记为推送失败
     */
    public OfflineMessage markAsFailed() {
        return new OfflineMessage(
                this.id, this.userId, this.messageId, this.pushChannel, PushStatus.FAILED, this.retryCount,
                this.maxRetryCount);
    }

    /**
     * 增加重试次数
     *
     * @throws IllegalStateException 已达最大重试次数
     */
    public OfflineMessage incrementRetry() {
        if (!canRetry()) {
            throw new IllegalStateException("已达最大重试次数：" + this.maxRetryCount);
        }
        return new OfflineMessage(
                this.id, this.userId, this.messageId, this.pushChannel, this.pushStatus, this.retryCount + 1,
                this.maxRetryCount);
    }
}
