package com.cartethyia.easyorange.order.application.saga.support;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.aggregate.OrderCreateSpec;
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
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 订单创建执行器。
 * <p>
 * 负责订单创建和支付创建的具体执行逻辑，作为 {@code CreateOrderSaga} 的支持组件。
 * 职责分离：本类只做执行，不涉及 Saga 编排、分布式锁、补偿等横切关注点。
 */
@Component
@RequiredArgsConstructor
public class OrderCreationExecutor {

    private static final String DEFAULT_PAYMENT_METHOD = "1";
    private static final String DEFAULT_ADDRESS = "未指定";

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    private final PaymentGatewayPort paymentGatewayPort;
    private final OrderCachePort<OrderVO> orderCachePort;
    private final OrderPreparationService preparationService;
    private final IdGenerator idGenerator;

    /**
     * 创建订单。
     *
     * @param command 创建订单命令
     * @return 创建结果（聚合根 + 领域事件）
     */
    public Order.OrderTransition<OrderCreatedEvent> createOrder(CreateOrderCommand command) {
        String buyerId = SecurityContextUtil.getCurrentUserIdOrThrow();

        // 准备订单项数据
        OrderPreparationService.PreparationResult preparation =
            preparationService.prepareOrderItems(command.items(), buyerId);

        // 创建订单聚合根（通过 spec record 收敛 7 个参数）
        Order.OrderTransition<OrderCreatedEvent> result = Order.createOrder(
            new OrderCreateSpec(
                idGenerator.generateId(),
                UserId.of(buyerId),
                preparation.sellerId(),
                preparation.orderItems(),
                Address.of(resolveAddress(command)),
                Phone.of(command.phone()),
                command.remark()
            )
        );

        // 保存并发布事件
        orderRepository.save(result.aggregate());
        eventPublisher.publish(result.event());

        return result;
    }

    /**
     * 创建支付。
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
                Optional.ofNullable(command.paymentMethod()).orElse(DEFAULT_PAYMENT_METHOD),
                "ORDER",
                "订单支付"
            ));
        } catch (Exception e) {
            throw new PaymentGatewayAdapterException(
                "支付创建失败 orderId=" + orderEvent.orderId() + ": " + e.getMessage(), e);
        }
    }

    /**
     * 清除资产方订单缓存。
     *
     * @param sellerId 资产方 ID
     */
    public void evictSellerCache(String sellerId) {
        orderCachePort.evictSellerOrders(sellerId);
    }

    /**
     * 解析地址：如果未指定则返回默认值。
     */
    private static String resolveAddress(CreateOrderCommand command) {
        return StringUtils.hasText(command.address()) ? command.address() : DEFAULT_ADDRESS;
    }
}