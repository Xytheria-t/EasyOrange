package com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;

@TableName("eo_product_audit_log")
public class ProductAuditLogDO extends BaseDO {

    private String productId;
    private String operatorId;
    private String operatorName;
    private Integer action;
    private String reason;
    private String auditDimensions;
    private Integer beforeStatus;
    private Integer afterStatus;
    private String remark;

    public ProductAuditLogDO() {
    }

    public ProductAuditLogDO(String productId, String operatorId, String operatorName, Integer action,
                              String reason, String auditDimensions, Integer beforeStatus,
                              Integer afterStatus, String remark) {
        this.productId = productId;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
        this.action = action;
        this.reason = reason;
        this.auditDimensions = auditDimensions;
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.remark = remark;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Integer getAction() {
        return action;
    }

    public void setAction(Integer action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAuditDimensions() {
        return auditDimensions;
    }

    public void setAuditDimensions(String auditDimensions) {
        this.auditDimensions = auditDimensions;
    }

    public Integer getBeforeStatus() {
        return beforeStatus;
    }

    public void setBeforeStatus(Integer beforeStatus) {
        this.beforeStatus = beforeStatus;
    }

    public Integer getAfterStatus() {
        return afterStatus;
    }

    public void setAfterStatus(Integer afterStatus) {
        this.afterStatus = afterStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String productId;
        private String operatorId;
        private String operatorName;
        private Integer action;
        private String reason;
        private String auditDimensions;
        private Integer beforeStatus;
        private Integer afterStatus;
        private String remark;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public Builder operatorId(String operatorId) {
            this.operatorId = operatorId;
            return this;
        }

        public Builder operatorName(String operatorName) {
            this.operatorName = operatorName;
            return this;
        }

        public Builder action(Integer action) {
            this.action = action;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder auditDimensions(String auditDimensions) {
            this.auditDimensions = auditDimensions;
            return this;
        }

        public Builder beforeStatus(Integer beforeStatus) {
            this.beforeStatus = beforeStatus;
            return this;
        }

        public Builder afterStatus(Integer afterStatus) {
            this.afterStatus = afterStatus;
            return this;
        }

        public Builder remark(String remark) {
            this.remark = remark;
            return this;
        }

        public ProductAuditLogDO build() {
            ProductAuditLogDO entity = new ProductAuditLogDO(productId, operatorId, operatorName, action,
                    reason, auditDimensions, beforeStatus, afterStatus, remark);
            if (id != null) {
                entity.setId(id);
            }
            return entity;
        }
    }
}
