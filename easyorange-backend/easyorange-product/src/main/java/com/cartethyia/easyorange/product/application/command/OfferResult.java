package com.cartethyia.easyorange.product.application.command;

import java.math.BigDecimal;

/**
 * 议价处理结果 — 包含决策类型、话术、还价金额和订单 ID。
 *
 * @param decisionType  决策类型：ACCEPT / COUNTER / REJECT
 * @param message       LLM 生成的议价话术
 * @param counterPrice  还价金额（COUNTER 时非空）
 * @param orderId       订单 ID（ACCEPT 时非空）
 */
public record OfferResult(
        String decisionType,
        String message,
        BigDecimal counterPrice,
        Long orderId
) {}
