package com.cartethyia.easyorange.product.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.cartethyia.easyorange.common.enums.BaseCodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.Set;
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

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    // === 状态机：单一事实来源 ===
    // 键为当前状态，值为允许到达的目标状态；各转换的触发动作见行内注释。
    private static final Map<ProductStatus, Set<ProductStatus>> ALLOWED_TRANSITIONS = Map.of(
            DRAFT, Set.of(PENDING_REVIEW, ONLINE), // 提交审核 submitForReview / 管理员直接上架 putOnline（绕过审核）
            PENDING_REVIEW, Set.of(ONLINE, REJECTED), // 审核通过 approve / 审核拒绝 reject
            REJECTED, Set.of(PENDING_REVIEW), // 重新提交审核 submitForReview（循环）
            ONLINE, Set.of(OFFLINE, SOLD), // 下架 takeOffline / 标记售出 markAsSold（订单完成时触发）
            OFFLINE, Set.of(ONLINE) // 重新上架 putOnline（relist）
            );

    public static ProductStatus fromCode(String code) {
        return BaseCodeEnum.fromCode(ProductStatus.class, code);
    }

    /**
     * 从当前状态到目标状态的转换是否合法。
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canTransitionTo(ProductStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
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
