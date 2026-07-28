package com.cartethyia.easyorange.message.domain.aggregate;

import com.cartethyia.easyorange.message.constant.MessageConstant;

/**
 * 消息模板聚合根 —— 不可变对象
 * <p>
 * 核心不变量：
 * <ul>
 *   <li>模板编码必须唯一</li>
 *   <li>模板只有启用/禁用两种状态</li>
 * </ul>
 */
public class MessageTemplate {

    private final String id;
    private final String templateCode;
    private final String templateName;
    private final String templateType;
    private final String title;
    private final String content;
    private final String variables;
    private final Integer status;
    private final String remark;

    private MessageTemplate(String id, String templateCode, String templateName,
                                      String templateType, String title, String content,
                                      String variables, Integer status, String remark) {
        this.id = id;
        this.templateCode = templateCode;
        this.templateName = templateName;
        this.templateType = templateType;
        this.title = title;
        this.content = content;
        this.variables = variables;
        this.status = status;
        this.remark = remark;
    }

    // ==================== Getters ====================

    public String id() { return id; }
    public String templateCode() { return templateCode; }
    public String templateName() { return templateName; }
    public String templateType() { return templateType; }
    public String title() { return title; }
    public String content() { return content; }
    public String variables() { return variables; }
    public Integer status() { return status; }
    public String remark() { return remark; }

    // ==================== Factory ====================

    /**
     * 创建消息模板
     */
    public static MessageTemplate create(String templateCode, String templateName,
                                                    String templateType, String title,
                                                    String content, String variables, String remark) {
        return new MessageTemplate(null, templateCode, templateName,
                templateType, title, content, variables,
                MessageConstant.TEMPLATE_STATUS_ENABLED, remark);
    }

    // ==================== Reconstruction ====================

    /**
     * 从持久层原始数据重建聚合根
     */
    public static MessageTemplate fromRaw(String id, String templateCode, String templateName,
                                                     String templateType, String title, String content,
                                                     String variables, Integer status, String remark) {
        return new MessageTemplate(id, templateCode, templateName,
                templateType, title, content, variables, status, remark);
    }

    // ==================== Predicates ====================

    public boolean isEnabled() {
        return this.status != null && this.status == MessageConstant.TEMPLATE_STATUS_ENABLED;
    }

    // ==================== State Transitions ====================

    /**
     * 启用模板
     */
    public MessageTemplate enable() {
        return new MessageTemplate(this.id, this.templateCode, this.templateName,
                this.templateType, this.title, this.content,
                this.variables, MessageConstant.TEMPLATE_STATUS_ENABLED, this.remark);
    }

    /**
     * 禁用模板
     */
    public MessageTemplate disable() {
        return new MessageTemplate(this.id, this.templateCode, this.templateName,
                this.templateType, this.title, this.content,
                this.variables, MessageConstant.STATUS_DISABLED, this.remark);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageTemplate other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "MessageTemplate{id=" + id + ", templateCode=" + templateCode + ", status=" + status + "}";
    }
}
