package com.cartethyia.easyorange.product.domain.enums;

import jakarta.annotation.Nullable;
import java.util.Arrays;

public enum ProductStatus {

    DRAFT(0, "草稿"),
    PENDING_REVIEW(4, "待审核"),
    REJECTED(5, "已驳回"),
    ONLINE(1, "上架"),
    SOLD(2, "已售出"),
    OFFLINE(3, "下架");

    private final Integer code;
    private final String desc;

    ProductStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * Resolves the enum value from its integer code.
     *
     * @param code the integer code (may be {@code null})
     * @return the matching enum value, or {@code null} if code is null or not recognized
     */
    @Nullable
    public static ProductStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(v -> v.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public static String getDescByCode(Integer code) {
        ProductStatus status = fromCode(code);
        return status != null ? status.getDesc() : "未知状态";
    }

    public boolean isDraft() {
        return this == DRAFT;
    }

    public boolean isOnline() {
        return this == ONLINE;
    }

    public boolean isSold() {
        return this == SOLD;
    }

    public boolean isOffline() {
        return this == OFFLINE;
    }

    public boolean canPutOnline() {
        return this == DRAFT || this == OFFLINE;
    }

    public boolean canTakeOffline() {
        return this == ONLINE;
    }

    public boolean canMarkAsSold() {
        return this == ONLINE;
    }

    public boolean canDelete() {
        return this != SOLD;
    }

    public boolean canSubmitForReview() {
        return this == DRAFT || this == REJECTED;
    }

    public boolean canApprove() {
        return this == PENDING_REVIEW;
    }

    public boolean canReject() {
        return this == PENDING_REVIEW;
    }
}
