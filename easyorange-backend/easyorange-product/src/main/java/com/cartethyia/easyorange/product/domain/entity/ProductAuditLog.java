package com.cartethyia.easyorange.product.domain.entity;

import java.time.LocalDateTime;

public class ProductAuditLog {

    private String id;
    private String productId;
    private String operatorId;
    private String operatorName;
    private String action;
    private String reason;
    private String auditDimensions;
    private String beforeStatus;
    private String afterStatus;
    private String remark;
    private LocalDateTime createTime;

    private ProductAuditLog(String productId, String operatorId, String operatorName,
                            String action, String reason, String auditDimensions,
                            String beforeStatus, String afterStatus, String remark) {
        this.productId = productId;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
        this.action = action;
        this.reason = reason;
        this.auditDimensions = auditDimensions;
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.remark = remark;
        this.createTime = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getProductId() { return productId; }
    public String getOperatorId() { return operatorId; }
    public String getOperatorName() { return operatorName; }
    public String getAction() { return action; }
    public String getReason() { return reason; }
    public String getAuditDimensions() { return auditDimensions; }
    public String getBeforeStatus() { return beforeStatus; }
    public String getAfterStatus() { return afterStatus; }
    public String getRemark() { return remark; }
    public LocalDateTime getCreateTime() { return createTime; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String productId;
        private String operatorId;
        private String operatorName;
private String action;
        private String reason;
        private String auditDimensions;
        private String beforeStatus;
        private String afterStatus;
        private String remark;

        public Builder productId(String productId) { this.productId = productId; return this; }
        public Builder operatorId(String operatorId) { this.operatorId = operatorId; return this; }
        public Builder operatorName(String operatorName) { this.operatorName = operatorName; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder auditDimensions(String auditDimensions) { this.auditDimensions = auditDimensions; return this; }
        public Builder beforeStatus(String beforeStatus) { this.beforeStatus = beforeStatus; return this; }
        public Builder afterStatus(String afterStatus) { this.afterStatus = afterStatus; return this; }
        public Builder remark(String remark) { this.remark = remark; return this; }

        public ProductAuditLog build() {
            return new ProductAuditLog(productId, operatorId, operatorName, action,
                    reason, auditDimensions, beforeStatus, afterStatus, remark);
        }
    }
}
