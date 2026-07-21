package com.cartethyia.easyorange.product.domain.enums;

import jakarta.annotation.Nullable;
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

    /**
     * Resolves the enum value from its integer code.
     *
     * @param code the integer code (may be {@code null})
     * @return the matching enum value, or {@code null} if code is null
     * @throws IllegalArgumentException if code is non-null but not recognized
     */
    @Nullable
    public static ProductReportStatus fromCode(Integer code) {
        if (code == null) return null;
        return switch (code) {
            case 0 -> PENDING;
            case 1 -> PROCESSING;
            case 2 -> RESOLVED;
            case 3 -> DISMISSED;
            default -> throw new IllegalArgumentException("Unknown ProductReportStatus code: " + code);
        };
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
