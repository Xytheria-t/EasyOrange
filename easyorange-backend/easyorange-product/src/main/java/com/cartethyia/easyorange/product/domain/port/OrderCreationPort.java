package com.cartethyia.easyorange.product.domain.port;

import java.math.BigDecimal;

/**
 * 订单创建端口 — AI 议价接受后创建订单。
 * <p>
 * 由 application 模块中的适配器实现，调用 order 模块的 OrderCommandHandler。
 */
public interface OrderCreationPort {

    /**
     * AI 议价接受后创建订单。
     *
     * @param buyerId     买家 ID
     * @param productId   商品 ID
     * @param agreedPrice 成交价
     * @return 订单 ID
     */
    Long createOrder(Long buyerId, Long productId, BigDecimal agreedPrice);
}
