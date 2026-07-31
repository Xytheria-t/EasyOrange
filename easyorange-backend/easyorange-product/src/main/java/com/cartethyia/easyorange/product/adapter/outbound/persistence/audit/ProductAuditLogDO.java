package com.cartethyia.easyorange.product.adapter.outbound.persistence.audit;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@TableName("eo_product_audit_log")
public class ProductAuditLogDO extends BaseDO {

    private String productId;
    private String operatorId;
    private String operatorName;
    private String action;
    private String reason;
    private String auditDimensions;
    private String beforeStatus;
    private String afterStatus;
    private String remark;
}
