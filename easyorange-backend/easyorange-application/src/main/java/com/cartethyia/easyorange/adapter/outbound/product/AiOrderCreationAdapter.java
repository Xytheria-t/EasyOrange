package com.cartethyia.easyorange.adapter.outbound.product;

import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.application.command.OrderCommandHandler;
import com.cartethyia.easyorange.product.domain.port.OrderCreationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 托管寄售订单创建适配器 — 实现 {@link OrderCreationPort}。
 * <p>
 * AI 议价接受后，通过 order 模块的 {@link OrderCommandHandler} 创建订单。
 */
@Primary
@Component
@RequiredArgsConstructor
public class AiOrderCreationAdapter implements OrderCreationPort {

    private final OrderCommandHandler orderCommandHandler;

    @Override
    public Long createOrder(Long buyerId, Long productId, BigDecimal agreedPrice) {
        // AI 工作流没有 JWT 上下文，需设置买家身份供 order 模块读取
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(buyerId, null, List.of()));
        try {
            CreateOrderCommand command = CreateOrderCommand.builder()
                    .items(List.of(CreateOrderCommand.CreateOrderItem.builder()
                            .productId(productId)
                            .quantity(1)
                            .build()))
                    .agreedPrice(agreedPrice)
                    .build();
            CreateOrderResult result = orderCommandHandler.handle(command);
            return result.orderId();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
