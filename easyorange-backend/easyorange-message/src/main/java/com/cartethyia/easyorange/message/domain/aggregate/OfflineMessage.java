package com.cartethyia.easyorange.message.domain.aggregate;

import com.cartethyia.easyorange.message.domain.enums.PushStatus;

/**
 * 离线消息聚合根 —— 不可变 record。
 * <p>
 * 做什么：接收方离线时把「待补推的站内信」落一行，上线时补推并标记 PUSHED。
 * <p>
 * 取舍：状态机只有 PENDING → PUSHED 一条边。<b>没有重试机制</b>——补推失败（目标消息不存在、
 * 非系统通知）时保持 PENDING，等用户下次上线再补推一次；没有重试计数、上限与退避，
 * 因为站内信本身不丢（行还在库里，前端拉列表照样看得到），重试计数只会造出「失败几次就放弃」
 * 的假象而不带来可靠性。
 * <p>
 * 边界：PENDING 行只在线上补推路径被消费（{@code OfflineMessageAppService#replayPending}）；
 * FAILED 枚举值当前无写入方，仅为能读懂这类历史行而保留。
 */
public record OfflineMessage(String id, String userId, String messageId, String pushChannel, PushStatus pushStatus) {

    // ── 工厂方法 ──

    /**
     * 创建离线消息（PENDING）。
     *
     * @param id 离线消息 ID，由应用层 {@code IdGenerator} 生成（{@code BaseDO.id} 为 {@code IdType.INPUT}，数据库不回填）
     */
    public static OfflineMessage create(String id, String userId, String messageId, String pushChannel) {
        return new OfflineMessage(id, userId, messageId, pushChannel, PushStatus.PENDING);
    }

    // ── 重建 ──

    /**
     * 从持久层原始数据重建聚合根
     */
    public static OfflineMessage fromRaw(
            String id, String userId, String messageId, String pushChannel, PushStatus pushStatus) {
        return new OfflineMessage(id, userId, messageId, pushChannel, pushStatus);
    }

    // ── 状态迁移 ──

    /**
     * 标记为已推送（补推成功）
     */
    public OfflineMessage markAsPushed() {
        return new OfflineMessage(this.id, this.userId, this.messageId, this.pushChannel, PushStatus.PUSHED);
    }
}
