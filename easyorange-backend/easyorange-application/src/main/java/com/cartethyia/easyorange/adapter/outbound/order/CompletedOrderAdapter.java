package com.cartethyia.easyorange.adapter.outbound.order;

import com.cartethyia.easyorange.order.application.port.query.OrderQueryRepository;
import com.cartethyia.easyorange.product.domain.port.CompletedOrderPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 已完成订单查询适配器 —— 实现 product 模块的评价资格 ACL 端口，委派 order 模块查询端口。
 * <p>
 * product 不依赖 order，实现落在组合层，与 {@code SellerInfoAdapter} 同一范式。
 */
@Primary
@Component
@RequiredArgsConstructor
public class CompletedOrderAdapter implements CompletedOrderPort {

    private final OrderQueryRepository orderQueryRepository;

    @Override
    public Optional<String> findCompletedOrderId(String buyerId, String productId) {
        return orderQueryRepository.findCompletedOrderId(buyerId, productId);
    }
}
