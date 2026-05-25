package com.cartethyia.easyorange.product.domain.entity;

import java.time.LocalDateTime;

public class ProductAuditLog {

    private Long id;
    private Long productId;
    private Long operatorId;
    private String operatorName;
    private Integer action;
    private String reason;
    private String auditDimensions;
    private Integer beforeStatus;
    private Integer afterStatus;
    private String remark;
    private LocalDateTime createTime;

    private ProductAuditLog(Long productId, Long operatorId, String operatorName,
                            Integer action, String reason, String auditDimensions,
                            Integer beforeStatus, Integer afterStatus, String remark) {
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

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Long getOperatorId() { return operatorId; }
    public String getOperatorName() { return operatorName; }
    public Integer getAction() { return action; }
    public String getReason() { return reason; }
    public String getAuditDimensions() { return auditDimensions; }
    public Integer getBeforeStatus() { return beforeStatus; }
    public Integer getAfterStatus() { return afterStatus; }
    public String getRemark() { return remark; }
    public LocalDateTime getCreateTime() { return createTime; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long productId;
        private Long operatorId;
        private String operatorName;
        private Integer action;
        private String reason;
        private String auditDimensions;
        private Integer beforeStatus;
        private Integer afterStatus;
        private String remark;

        public Builder productId(Long productId) { this.productId = productId; return this; }
        public Builder operatorId(Long operatorId) { this.operatorId = operatorId; return this; }
        public Builder operatorName(String operatorName) { this.operatorName = operatorName; return this; }
        public Builder action(Integer action) { this.action = action; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder auditDimensions(String auditDimensions) { this.auditDimensions = auditDimensions; return this; }
        public Builder beforeStatus(Integer beforeStatus) { this.beforeStatus = beforeStatus; return this; }
        public Builder afterStatus(Integer afterStatus) { this.afterStatus = afterStatus; return this; }
        public Builder remark(String remark) { this.remark = remark; return this; }

        public ProductAuditLog build() {
            return new ProductAuditLog(productId, operatorId, operatorName, action,
                    reason, auditDimensions, beforeStatus, afterStatus, remark);
        }
    }
}
