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
public class MessageSubscription {

    private final String id;
    private final String userId;
    private final String messageType;
    private final String pushChannel;
    private final Boolean enabled;

    private MessageSubscription(String id, String userId, String messageType,
                                          String pushChannel, Boolean enabled) {
        this.id = id;
        this.userId = userId;
        this.messageType = messageType;
        this.pushChannel = pushChannel;
        this.enabled = enabled;
    }

    // ==================== Getters ====================

    public String id() { return id; }
    public String userId() { return userId; }
    public String messageType() { return messageType; }
    public String pushChannel() { return pushChannel; }
    public Boolean enabled() { return enabled; }

    // ==================== Factory ====================

    /**
     * 创建消息订阅
     */
    public static MessageSubscription create(String userId, String messageType,
                                                       String pushChannel, Boolean enabled) {
        return new MessageSubscription(null, userId, messageType, pushChannel, enabled);
    }

    // ==================== Reconstruction ====================

    /**
     * 从持久层原始数据重建聚合根
     */
    public static MessageSubscription fromRaw(String id, String userId, String messageType,
                                                        String pushChannel, Boolean enabled) {
        return new MessageSubscription(id, userId, messageType, pushChannel, enabled);
    }

    // ==================== Predicates ====================

    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.enabled);
    }

    // ==================== State Transitions ====================

    /**
     * 启用订阅
     */
    public MessageSubscription enable() {
        return new MessageSubscription(this.id, this.userId, this.messageType,
                this.pushChannel, true);
    }

    /**
     * 禁用订阅
     */
    public MessageSubscription disable() {
        return new MessageSubscription(this.id, this.userId, this.messageType,
                this.pushChannel, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageSubscription other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "MessageSubscription{id=" + id + ", userId=" + userId + ", messageType=" + messageType + ", enabled=" + enabled + "}";
    }
}
