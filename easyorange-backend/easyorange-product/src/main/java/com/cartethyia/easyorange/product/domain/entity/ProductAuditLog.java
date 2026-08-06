package com.cartethyia.easyorange.product.domain.entity;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductAuditLog {

    private String id;
    private final String productId;
    private final String operatorId;
    private final String operatorName;
    private final String action;
    private final String reason;
    private final String auditDimensions;
    private final String beforeStatus;
    private final String afterStatus;
    private final String remark;

    @Builder.Default
    private final LocalDateTime createTime = LocalDateTime.now();
}
