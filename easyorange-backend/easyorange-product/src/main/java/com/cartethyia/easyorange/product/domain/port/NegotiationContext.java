package com.cartethyia.easyorange.product.domain.port;

import java.math.BigDecimal;

/**
 * 议价上下文 — 用于 LLM 话术生成的信息载体。
 *
 * @param decisionType  决策类型：ACCEPT / COUNTER / REJECT
 * @param acceptedPrice 接受价格（ACCEPT 时非空）
 * @param counterPrice  还价金额（COUNTER 时非空）
 * @param reason        决策原因
 * @param productName   商品名称
 * @param buyerOffer    买家出价
 */
public record NegotiationContext(
        String decisionType,
        BigDecimal acceptedPrice,
        BigDecimal counterPrice,
        String reason,
        String productName,
        BigDecimal buyerOffer
) {}
