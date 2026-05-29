package com.cartethyia.easyorange.product.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductReportStatus {

    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    RESOLVED(2, "已解决"),
    DISMISSED(3, "已驳回");

    private final Integer code;
    private final String desc;

    public static ProductReportStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ProductReportStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isProcessing() {
        return this == PROCESSING;
    }

    public boolean isResolved() {
        return this == RESOLVED;
    }

    public boolean canProcess() {
        return this == PENDING;
    }

    public boolean canResolve() {
        return this == PROCESSING;
    }

    public boolean canDismiss() {
        return this == PENDING;
    }
}
