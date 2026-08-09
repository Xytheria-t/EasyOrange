package com.cartethyia.easyorange.product.domain.enums;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 商品状态机动作 — 商品生命周期所有合法转换的**唯一事实来源**。
 * <p>
 * 每个动作声明：前置状态集合（sources）、目标状态（target）。
 * {@link ProductStatus#canTransitionTo(ProductStatus)} 由此派生，聚合根统一经
 * {@code Product#transitionTo(ProductAction)} 守卫。
 * <pre>
 * DRAFT ──submitForReview──→ PENDING_REVIEW ──approve──→ ONLINE ⇄ takeOffline/putOnline ⇄ OFFLINE
 *   │                           │            └──reject──→ REJECTED ──submitForReview──→ PENDING_REVIEW
 *   └───────────putOnline────────┘                        （REJECTED 可循环提交审核）
 * ONLINE ──markAsSold──→ SOLD（终端）
 * </pre>
 * <ul>
 *   <li>{@code SUBMIT_FOR_REVIEW}：提交审核，草稿或已驳回状态</li>
 *   <li>{@code PUT_ONLINE}：上架，草稿（管理员直上架绕过审核）或已下架（重新上架）</li>
 *   <li>{@code APPROVE}：审核通过（进入 ONLINE 前还需过 {@code Product.validateOnline()} 不变量）</li>
 *   <li>{@code MARK_AS_SOLD}：标记售出（订单完成时触发），仅上架状态</li>
 * </ul>
 */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public enum ProductAction {

    SUBMIT_FOR_REVIEW("提交审核", Set.of(ProductStatus.DRAFT, ProductStatus.REJECTED), ProductStatus.PENDING_REVIEW),
    PUT_ONLINE("上架", Set.of(ProductStatus.DRAFT, ProductStatus.OFFLINE), ProductStatus.ONLINE),
    APPROVE("审核通过", Set.of(ProductStatus.PENDING_REVIEW), ProductStatus.ONLINE),
    REJECT("审核拒绝", Set.of(ProductStatus.PENDING_REVIEW), ProductStatus.REJECTED),
    TAKE_OFFLINE("下架", Set.of(ProductStatus.ONLINE), ProductStatus.OFFLINE),
    MARK_AS_SOLD("标记售出", Set.of(ProductStatus.ONLINE), ProductStatus.SOLD);

    /** 动作名称（用于日志/提示） */
    private final String actionName;
    /** 允许触发该动作的前置状态集合 */
    private final Set<ProductStatus> sources;
    /** 动作执行后的目标状态 */
    private final ProductStatus target;

    /**
     * 当前状态是否允许触发该动作。
     */
    public boolean canApply(ProductStatus currentStatus) {
        return sources.contains(currentStatus);
    }
}
