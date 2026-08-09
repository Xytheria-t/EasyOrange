package com.cartethyia.easyorange.product.domain.enums;

import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductStatus implements BaseCodeEnum {

    // 按生命周期顺序声明：草稿 → 待审核 → 已驳回 → 上架 → 下架 → 已售出（终端）
    DRAFT("DRAFT", "草稿"),
    PENDING_REVIEW("PENDING_REVIEW", "待审核"),
    REJECTED("REJECTED", "已驳回"),
    ONLINE("ONLINE", "上架"),
    OFFLINE("OFFLINE", "下架"),
    SOLD("SOLD", "已售出");

    @JsonValue
    private final String code;

    private final String desc;

    public static ProductStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(ProductStatus.class, code);
    }

    /**
     * 从当前状态到目标状态的转换是否合法。
     * <p>
     * 状态机合法转换的**唯一事实来源**是 {@link ProductAction}，本方法由此派生，
     * 避免两份状态机定义漂移。
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canTransitionTo(ProductStatus target) {
        return Arrays.stream(ProductAction.values())
                .anyMatch(action -> action.sources().contains(this) && action.target() == target);
    }

    /**
     * 终端状态：SOLD（已售出）无任何合法转换。
     */
    public boolean isTerminal() {
        return this == SOLD;
    }

    /**
     * Delete is not a status transition — the status field stays unchanged.
     * Allowed from all states except the terminal state SOLD (completed orders must retain the product record).
     */
    public boolean canDelete() {
        return !isTerminal();
    }

    /**
     * 库存恢复不变量（非状态转换）：商品离开可交易生命周期后不可再恢复库存。
     * SOLD 商品已售出无货可恢复；OFFLINE 商品已下架离开在售生命周期。
     */
    public boolean canRestoreStock() {
        return this != SOLD && this != OFFLINE;
    }
}
