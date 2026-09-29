package com.cartethyia.easyorange.product.domain.valueobject;

/**
 * 库存对账漂移 — 实际库存与最近一条流水的 stock_after 不一致；成因必是绕过流水落账或两者未同事务提交，
 * 都是缺陷信号：只告警后人工核对流水还原真相，不自动改写余额（自动修复会掩盖缺陷）。
 */
public record StockDrift(String productId, int actualStock, int ledgerStock) {}
