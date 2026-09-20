package com.cartethyia.easyorange.product.domain.port;

import java.util.Optional;

/**
 * 已完成订单查询端口（ACL）—— 评价资格校验：只有成交完成的买家才能评价该资产。
 * <p>
 * product 模块不依赖 order 模块，端口实现由组合层（easyorange-application 的出站适配器）提供，
 * 与 {@code SellerInfoPort} 同一范式。
 */
public interface CompletedOrderPort {

    /**
     * 查询买家针对该资产已完成交易的订单 ID。评价必须落到真实成交订单上，
     * 否则商品详情页公开的评分与评价列表可被无成交记录的刷评污染。
     *
     * @param buyerId   买家 ID
     * @param productId 资产 ID
     * @return 订单 ID；该买家没有该资产的已完成订单时返回 {@link Optional#empty()}
     */
    Optional<String> findCompletedOrderId(String buyerId, String productId);
}
