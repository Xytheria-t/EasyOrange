package com.cartethyia.easyorange.product.domain.enums;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductReportStatus implements BaseCodeEnum {

    PENDING("0", "待处理"),
    PROCESSING("1", "处理中"),
    RESOLVED("2", "已解决"),
    DISMISSED("3", "已驳回");

    @JsonValue
    private final String code;
    private final String desc;

    public static ProductReportStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(ProductReportStatus.class, code);
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
