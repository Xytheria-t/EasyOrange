package com.cartethyia.easyorange.product.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductReportStatus {

    PENDING("0", "待处理"),
    PROCESSING("1", "处理中"),
    RESOLVED("2", "已解决"),
    DISMISSED("3", "已驳回");

    @JsonValue
    private final String code;
    private final String desc;

    public static ProductReportStatus fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("ProductReportStatus code must not be null");
        }
        for (var status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ProductReportStatus code: " + code);
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
