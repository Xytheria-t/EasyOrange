package com.cartethyia.easyorange.order.application.saga.support;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.saga.PaymentGatewayAdapterException;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单创建执行器
 * <p>
 * 负责订单创建和支付创建的具体执行逻辑
 */
@Component
@RequiredArgsConstructor
public class OrderCreationExecutor {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    private final PaymentGatewayPort paymentGatewayPort;
    private final OrderCachePort<?> orderCachePort;
    private final OrderPreparationService preparationService;
    private final IdGenerator idGenerator;

    /**
     * 创建订单
     *
     * @param command 创建订单命令
     * @return 创建结果
     */
    public OrderAggregate.OrderCreatedResult createOrder(CreateOrderCommand command) {
        String buyerId = SecurityContextUtil.getCurrentUserIdOrThrow();

        // 准备订单项数据
        List<OrderPreparationService.OrderItemRequest> itemRequests = command.getItems().stream()
            .map(item -> new OrderPreparationService.OrderItemRequest(
                    item.getProductId(), item.getQuantity()))
            .toList();

        OrderPreparationService.PreparationResult preparation =
            preparationService.prepareOrderItems(itemRequests, buyerId);

        // 解析地址
        String resolvedAddress = resolveAddress(command);

        // 创建订单聚合根
        OrderAggregate.OrderCreatedResult result = OrderAggregate.createOrder(
            UserId.of(buyerId),
            UserId.of(preparation.sellerId()),
            preparation.orderItems(),
            Address.of(resolvedAddress),
            Phone.of(command.getPhone()),
            command.getRemark(),
            idGenerator.generateId()
        );

        // 保存并发布事件
        orderRepository.save(result.aggregate());
        eventPublisher.publish(result.event());

        return result;
    }

    /**
     * 创建支付
     *
     * @param orderEvent 订单创建事件
     * @param command    创建订单命令
     * @throws PaymentGatewayAdapterException 如果支付创建失败
     */
    public void createPayment(OrderCreatedEvent orderEvent, CreateOrderCommand command) {
        try {
            paymentGatewayPort.createPayment(new PaymentGatewayPort.CreatePaymentRequest(
                orderEvent.orderId(),
                orderEvent.totalAmount(),
                command.getPaymentMethod() != null ? command.getPaymentMethod() : 1,
                "ORDER",
                "订单支付"
            ));
        } catch (Exception e) {
            throw new PaymentGatewayAdapterException("支付创建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清除资产方订单缓存
     *
     * @param sellerId 资产方 ID
     */
    public void evictSellerCache(String sellerId) {
        orderCachePort.evictSellerOrders(sellerId);
    }

    /**
     * 解析地址
     */
    private String resolveAddress(CreateOrderCommand command) {
        if (command.getAddress() != null && !command.getAddress().isBlank()) {
            return command.getAddress();
        }
        return "未指定";
    }
}