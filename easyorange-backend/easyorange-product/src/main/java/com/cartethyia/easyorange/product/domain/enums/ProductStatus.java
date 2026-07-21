package com.cartethyia.easyorange.product.domain.enums;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

@Getter
@AllArgsConstructor
public enum ProductStatus {

    DRAFT(0, "草稿"),
    PENDING_REVIEW(4, "待审核"),
    REJECTED(5, "已驳回"),
    ONLINE(1, "上架"),
    SOLD(2, "已售出"),
    OFFLINE(3, "下架");

    // === State Machine: one source of truth for allowed transitions ===

    private static final Map<ProductStatus, Set<ProductStatus>> ALLOWED_TRANSITIONS = Map.of(
        DRAFT, Set.of(PENDING_REVIEW, ONLINE),
        PENDING_REVIEW, Set.of(ONLINE, REJECTED),
        REJECTED, Set.of(PENDING_REVIEW),
        ONLINE, Set.of(OFFLINE, SOLD),
        OFFLINE, Set.of(ONLINE)
    );

    private final int code;
    private final String desc;

    @Nullable
    public static ProductStatus fromCode(Integer code) {
        if (code == null) return null;
        return switch (code) {
            case 0 -> DRAFT;
            case 1 -> ONLINE;
            case 2 -> SOLD;
            case 3 -> OFFLINE;
            case 4 -> PENDING_REVIEW;
            case 5 -> REJECTED;
            default -> throw new IllegalArgumentException("Unknown ProductStatus code: " + code);
        };
    }

    public static String getDescByCode(Integer code) {
        try {
            var status = fromCode(code);
            return status != null ? status.getDesc() : "未知状态";
        } catch (IllegalArgumentException e) {
            return "未知状态";
        }
    }

    // === State Machine ===

    /**
     * Returns whether a transition from the current state to the given target state is allowed.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canTransitionTo(ProductStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    /**
     * Delete is not a status transition — the status field stays unchanged.
     * Allowed from all states except SOLD (completed orders must retain the product record).
     */
    public boolean canDelete() {
        return this != SOLD;
    }
}
