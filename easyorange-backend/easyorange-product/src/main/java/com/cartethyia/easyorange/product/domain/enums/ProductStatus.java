package com.cartethyia.easyorange.product.domain.enums;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

@Getter
@AllArgsConstructor
public enum ProductStatus implements BaseCodeEnum {

    DRAFT("0", "草稿"),
    ONLINE("1", "上架"),
    SOLD("2", "已售出"),
    OFFLINE("3", "下架"),
    PENDING_REVIEW("4", "待审核"),
    REJECTED("5", "已驳回");

    @JsonValue
    private final String code;
    private final String desc;

    // === State Machine: one source of truth for allowed transitions ===

    private static final Map<ProductStatus, Set<ProductStatus>> ALLOWED_TRANSITIONS = Map.of(
        DRAFT, Set.of(PENDING_REVIEW, ONLINE),
        PENDING_REVIEW, Set.of(ONLINE, REJECTED),
        REJECTED, Set.of(PENDING_REVIEW),
        ONLINE, Set.of(OFFLINE, SOLD),
        OFFLINE, Set.of(ONLINE)
    );

    public static ProductStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(ProductStatus.class, code);
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
