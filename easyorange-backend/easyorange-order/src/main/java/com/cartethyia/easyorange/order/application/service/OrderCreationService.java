package com.cartethyia.easyorange.order.application.service;

import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.port.LockPort;
import com.cartethyia.easyorange.order.domain.port.ProductOrderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 订单创建服务
 * <p>
 * 下单链路：分布式锁防超卖 → 准备商品数据 → 创建订单 → 同步扣减库存 → 创建支付，
 * 全部步骤运行在同一 {@code @Transactional} 事务内，任一步失败由数据库整体回滚兜底
 * （订单 / 库存 / 支付 / Outbox 事件原子提交）。
 * <p>
 * 一致性语义：本地单事务保证原子性；并发防超卖由 {@link LockPort} 承担；
 * 事件副作用经 Outbox 与应用事务同原子持久化。为何不使用 Saga 见 ADR-0007。
 * 异常不做二次包装，直接抛给 {@code GlobalExceptionHandler} 按错误码映射。
 */
@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private static final String ORDER_LOCK_PREFIX = "eo:order:lock:product:";
    private static final long LOCK_TRY_TIMEOUT_SECONDS = 10;

    private final LockPort lockPort;
    private final OrderCreationExecutor orderCreationExecutor;
    private final ProductOrderPort productOrderPort;

    /**
     * 执行订单创建
     */
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        return lockPort.executeWithLocks(
            buildLockKeys(command), LOCK_TRY_TIMEOUT_SECONDS, () -> createOrderFlow(command));
    }

    /**
     * 构建锁键列表 — 按 productId 排序避免死锁
     */
    private List<String> buildLockKeys(CreateOrderCommand command) {
        return command.items().stream()
            .map(CreateOrderCommand.CreateOrderItem::productId)
            .distinct()
            .sorted()
            .map(id -> ORDER_LOCK_PREFIX + id)
            .toList();
    }

    /**
     * 执行下单流程 — 全部步骤在同一事务内，失败由回滚兜底
     */
    private CreateOrderResult createOrderFlow(CreateOrderCommand command) {
        Transition<Order, OrderCreatedEvent> result = orderCreationExecutor.createOrder(command);

        // 同步扣减库存（同一事务，失败时随事务整体回滚）
        for (var item : command.items()) {
            productOrderPort.decreaseStock(item.productId(), item.quantity());
        }

        // 创建支付（同一事务，失败时随事务整体回滚）
        orderCreationExecutor.createPayment(result.event(), command);
        orderCreationExecutor.evictSellerCache(result.aggregate().sellerId().value());

        return new CreateOrderResult(result.aggregate().id().value(), result.aggregate().orderNo().value());
    }
}
