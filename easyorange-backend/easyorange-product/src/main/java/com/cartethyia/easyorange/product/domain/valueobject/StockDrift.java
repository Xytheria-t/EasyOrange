package com.cartethyia.easyorange.product.domain.valueobject;

/**
 * 库存对账漂移 — 资产的实际库存与最近一条流水的余额快照不一致。
 * <p>
 * 出现漂移只有两种可能：有库存变更绕过了流水落账，或流水与余额没有同事务提交。两种都是缺陷信号，
 * 由对账任务告警后人工核对流水还原真相，不自动改写余额（自动修复会掩盖缺陷）。
 *
 * @param productId   资产 ID
 * @param actualStock {@code eo_product.stock} 当前值
 * @param ledgerStock 最近一条流水的 {@code stock_after}
 */
public record StockDrift(String productId, int actualStock, int ledgerStock) {}
