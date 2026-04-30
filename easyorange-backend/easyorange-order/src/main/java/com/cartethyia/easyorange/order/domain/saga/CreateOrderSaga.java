package com.cartethyia.easyorange.order.domain.saga;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.outbound.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.port.outbound.ProductInventoryPort;
import com.cartethyia.easyorange.order.domain.port.outbound.ProductInventoryPort.ProductSnapshot;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.Money;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.ProductId;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.enums.OrderStatus;
import com.cartethyia.easyorange.order.infrastructure.cache.OrderCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrderSaga {

    private final OrderRepository orderRepository;
    private final ProductInventoryPort productInventoryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final DomainEventPublisher eventPublisher;
    private final OrderCacheService orderCacheService;
    private final RedisCache redisCache;

    private static final String ORDER_LOCK_PREFIX = "eo:order:lock:product:";

    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResult execute(CreateOrderCommand command) {
        String lockKey = ORDER_LOCK_PREFIX + command.getProductId();
        String lockValue = UUID.randomUUID().toString();

        Boolean locked = redisCache.tryLock(lockKey, lockValue, 10, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new OrderDomainException("商品下单繁忙，请稍后重试");
        }

        try {
            return doExecute(command);
        } finally {
            redisCache.unlockIfValueMatches(lockKey, lockValue);
        }
    }

    private CreateOrderResult doExecute(CreateOrderCommand command) {
        List<CompensatingAction> compensations = new ArrayList<>();

        try {
            OrderAggregate.OrderCreatedResult createResult = createOrder(command);
            OrderAggregate aggregate = createResult.aggregate();
            OrderCreatedEvent orderEvent = createResult.event();
            compensations.add(() -> cancelOrder(aggregate.id()));

            paymentGatewayPort.createPayment(new PaymentGatewayPort.CreatePaymentRequest(
                    orderEvent.getOrderId(),
                    orderEvent.getAmount(),
                    command.getPaymentMethod() != null ? command.getPaymentMethod() : 1,
                    "ORDER",
                    "订单支付"
            ));

            orderCacheService.deleteSellerOrderCache(aggregate.sellerId().value());

            log.info("订单创建 Saga 完成 orderId={} orderNo={}", aggregate.id().value(), aggregate.orderNo().value());

            return new CreateOrderResult(aggregate.id().value(), aggregate.orderNo().value());

        } catch (Exception e) {
            log.error("订单创建 Saga 失败，执行补偿逻辑 command={}", command, e);
            compensate(compensations, e);
            throw new OrderCreationException("订单创建失败：" + e.getMessage(), e);
        }
    }

    private OrderAggregate.OrderCreatedResult createOrder(CreateOrderCommand command) {
        Long buyerId = SecurityContextUtil.getCurrentUserIdOrThrow();

        ProductSnapshot snapshot = productInventoryPort.getSnapshot(command.getProductId())
                .orElseThrow(() -> new OrderDomainException("商品不存在"));
        BizRequire.requireTrue(snapshot.isOnline(), "商品已下架");
        BizRequire.ne(snapshot.sellerId(), buyerId, "不能购买自己的商品");
        BizRequire.requireTrue(snapshot.hasStock(), "商品库存不足");

        OrderAggregate.OrderCreatedResult result = OrderAggregate.createOrder(
                UserId.of(buyerId),
                UserId.of(snapshot.sellerId()),
                ProductId.of(command.getProductId()),
                Money.of(snapshot.price()),
                Address.of(command.getAddress()),
                Phone.of(command.getPhone()),
                command.getRemark()
        );

        orderRepository.save(result.aggregate());
        eventPublisher.publish(result.event());
        log.info("Saga: 订单创建成功 orderId={}", result.aggregate().id().value());

        return result;
    }

    @FunctionalInterface
    private interface CompensatingAction {
        void compensate();
    }

    private void compensate(List<CompensatingAction> compensations, Exception cause) {
        for (int i = compensations.size() - 1; i >= 0; i--) {
            try {
                compensations.get(i).compensate();
            } catch (Exception e) {
                log.error("补偿操作失败 index={}", i, e);
            }
        }
    }

    private void cancelOrder(OrderId orderId) {
        try {
            orderRepository.findById(orderId)
                    .ifPresent(aggregate -> {
                        if (OrderStatus.canCancel(aggregate.status().getCode())) {
                            OrderAggregate.OrderCancelledResult result = aggregate.cancel("Saga 补偿取消");
                            orderRepository.update(result.aggregate());
                            log.info("Saga: 订单补偿成功 orderId={}", orderId.value());
                        } else {
                            log.warn("Saga: 订单状态不允许取消补偿 orderId={} status={}", orderId.value(), aggregate.status());
                        }
                    });
        } catch (Exception e) {
            log.error("Saga: 订单补偿失败 orderId={}", orderId.value(), e);
            throw new OrderDomainException("订单补偿失败", e);
        }
    }
}
