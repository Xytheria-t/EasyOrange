package com.cartethyia.easyorange.order.application.saga.support;

import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.event.OrderCancelledEvent;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.saga.SagaException;
import com.cartethyia.easyorange.order.domain.saga.SagaState;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单补偿服务
 * <p>
 * 负责 Saga 失败时的订单补偿操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompensationService {

    private final OrderRepository orderRepository;
    private final OrderCachePort<?> orderCachePort;

    /**
     * 执行补偿操作
     *
     * @param compensations 补偿操作列表
     * @param cause         导致补偿的原始异常
     * @return 补偿执行日志
     */
    public String executeCompensations(List<CompensatingAction> compensations, Exception cause) {
        log.warn("开始执行补偿逻辑，共 {} 个补偿操作，原因: {}", compensations.size(), cause.getMessage());

        List<String> compensationResults = new ArrayList<>();

        // 逆序执行补偿操作
        for (int i = compensations.size() - 1; i >= 0; i--) {
            int stepIndex = compensations.size() - i;
            CompensatingAction action = compensations.get(i);

            try {
                action.compensate();
                compensationResults.add(String.format("Step %d: SUCCESS", stepIndex));
            } catch (SagaException e) {
                compensationResults.add(String.format("Step %d: FAILED - %s", stepIndex, e.getMessage()));
                log.error("补偿操作失败 step={}，将继续执行其他补偿", stepIndex, e);
            } catch (Exception e) {
                compensationResults.add(String.format("Step %d: FAILED - %s", stepIndex, e.getMessage()));
                log.error("补偿操作失败 step={}，将继续执行其他补偿", stepIndex, e);
            }
        }

        log.warn("补偿逻辑执行完成，结果: {}", compensationResults);
        return String.join("; ", compensationResults);
    }

    /**
     * 取消订单（用于补偿）
     *
     * @param orderId 订单 ID
     * @throws SagaException 如果取消失败
     */
    public void cancelOrder(OrderId orderId) {
        try {
            orderRepository.findById(orderId)
                .ifPresentOrElse(
                    aggregate -> cancelIfPossible(aggregate),
                    () -> log.warn("Saga: 订单不存在，无需补偿 orderId={}", orderId.value())
                );
        } catch (OrderDomainException e) {
            throw new SagaException(
                null,
                SagaState.COMPENSATING,
                "订单补偿失败: " + e.getMessage(),
                e
            );
        } catch (Exception e) {
            log.error("Saga: 订单补偿失败 orderId={}", orderId.value(), e);
            throw new SagaException(
                null,
                SagaState.COMPENSATING,
                "订单补偿失败",
                e
            );
        }
    }

    /**
     * 如果订单可以取消，则执行取消操作
     */
    private void cancelIfPossible(OrderAggregate aggregate) {
        if (aggregate.canCancel()) {
            OrderAggregate.OrderTransition<OrderCancelledEvent> result = aggregate.cancel("Saga 补偿取消");
            orderRepository.update(result.aggregate());
            orderCachePort.evictOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
            log.info("Saga: 订单补偿取消成功 orderId={}", aggregate.id().value());
        } else {
            log.warn("Saga: 订单状态不允许取消补偿 orderId={} status={}",
                aggregate.id().value(), aggregate.status());
        }
    }

    /**
     * 补偿操作接口
     */
    @FunctionalInterface
    public interface CompensatingAction {
        void compensate() throws SagaException;
    }
}