package com.cartethyia.easyorange.message.domain.port;

import java.math.BigDecimal;

/**
 * 议价处理端口 — message 模块调用 product 模块的出价处理。
 * <p>
 * 由 application 模块中的适配器实现，调用 product 模块的 OfferAppService。
 */
public interface OfferProcessingPort {

    /**
     * 议价处理结果
     *
     * @param decisionType  决策类型：ACCEPT / COUNTER / REJECT
     * @param message       LLM 生成的议价话术
     * @param counterPrice  还价金额（COUNTER 时非空）
     * @param orderId       订单 ID（ACCEPT 时非空）
     */
    record OfferResult(
            String decisionType,
            String message,
            BigDecimal counterPrice,
            Long orderId
    ) {}

    /**
     * 处理买家出价。
     *
     * @param buyerId    买家 ID
     * @param productId  商品 ID
     * @param offerPrice 出价金额
     * @return 议价处理结果
     */
    OfferResult processOffer(Long buyerId, Long productId, BigDecimal offerPrice);
}
