package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.product.domain.enums.StockChangeType;

/**
 * 一次库存变更的落账信息 — 库存流水（{@code eo_stock_ledger}）的一条记录。
 * <p>
 * 幂等键为 {@code (changeType, bizId, productId)}：携带业务单号（订单号）的变更只允许落账一次，
 * 重复投递撞唯一键即跳过；{@link StockChangeType#INIT} / {@link StockChangeType#ADJUST} 的 {@code bizId} 为
 * {@code null}（MySQL 唯一索引不约束 NULL），因此同一资产可多次人工调整。
 *
 * @param changeType 变更类型
 * @param bizId      业务单号（订单 ID），{@code null} 表示不参与幂等约束
 * @param productId  资产 ID
 * @param delta      库存变化量（正数增加 / 负数减少）
 * @param stockAfter 变更后库存余额，对账基准
 */
public record StockChange(StockChangeType changeType, String bizId, ProductId productId, int delta, int stockAfter) {

    public StockChange {
        BizRequire.notNull(changeType, "库存变更类型不能为空");
        BizRequire.notNull(productId, "资产 ID 不能为空");
        BizRequire.requireTrue(stockAfter >= 0, "变更后库存不能为负数: " + stockAfter);
        BizRequire.requireTrue(
                bizId != null || changeType == StockChangeType.INIT || changeType == StockChangeType.ADJUST,
                "下单扣减/库存恢复必须携带业务单号: " + changeType);
    }

    /** 下单扣减（扣减量为正数，落账记负）。 */
    public static StockChange decrease(String orderId, ProductId productId, int quantity, int stockAfter) {
        BizRequire.requireTrue(quantity > 0, "扣减数量必须大于 0: " + quantity);
        return new StockChange(StockChangeType.DECREASE, orderId, productId, -quantity, stockAfter);
    }

    /** 取消 / 退款恢复（恢复量为正数）。 */
    public static StockChange restore(String orderId, ProductId productId, int quantity, int stockAfter) {
        BizRequire.requireTrue(quantity > 0, "恢复数量必须大于 0: " + quantity);
        return new StockChange(StockChangeType.RESTORE, orderId, productId, quantity, stockAfter);
    }
}
