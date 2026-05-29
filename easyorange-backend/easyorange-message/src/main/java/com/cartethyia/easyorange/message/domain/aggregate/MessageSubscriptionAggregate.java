package com.cartethyia.easyorange.message.domain.aggregate;

/**
 * 消息订阅聚合根 —— 不可变对象
 * <p>
 * 核心不变量：
 * <ul>
 *   <li>每个用户每种消息类型每个推送渠道最多一个订阅</li>
 *   <li>订阅只有启用/禁用两种状态</li>
 * </ul>
 */
public class MessageSubscriptionAggregate {

    private final Long id;
    private final Long userId;
    private final String messageType;
    private final String pushChannel;
    private final Boolean enabled;

    private MessageSubscriptionAggregate(Long id, Long userId, String messageType,
                                          String pushChannel, Boolean enabled) {
        this.id = id;
        this.userId = userId;
        this.messageType = messageType;
        this.pushChannel = pushChannel;
        this.enabled = enabled;
    }

    // ==================== Getters ====================

    public Long id() { return id; }
    public Long userId() { return userId; }
    public String messageType() { return messageType; }
    public String pushChannel() { return pushChannel; }
    public Boolean enabled() { return enabled; }

    // ==================== Factory ====================

    /**
     * 创建消息订阅
     */
    public static MessageSubscriptionAggregate create(Long userId, String messageType,
                                                       String pushChannel, Boolean enabled) {
        return new MessageSubscriptionAggregate(null, userId, messageType, pushChannel, enabled);
    }

    // ==================== Reconstruction ====================

    /**
     * 从持久层原始数据重建聚合根
     */
    public static MessageSubscriptionAggregate fromRaw(Long id, Long userId, String messageType,
                                                        String pushChannel, Boolean enabled) {
        return new MessageSubscriptionAggregate(id, userId, messageType, pushChannel, enabled);
    }

    // ==================== Predicates ====================

    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.enabled);
    }

    // ==================== State Transitions ====================

    /**
     * 启用订阅
     */
    public MessageSubscriptionAggregate enable() {
        return new MessageSubscriptionAggregate(this.id, this.userId, this.messageType,
                this.pushChannel, true);
    }

    /**
     * 禁用订阅
     */
    public MessageSubscriptionAggregate disable() {
        return new MessageSubscriptionAggregate(this.id, this.userId, this.messageType,
                this.pushChannel, false);
    }
}
