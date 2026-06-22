package com.cartethyia.easyorange.order.application.saga;

import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.application.saga.support.*;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.saga.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建订单 Saga
 * <p>
 * 编排订单创建的分布式事务流程，包括：
 * 1. 创建订单
 * 2. 创建支付
 * <p>
 * 失败时自动执行补偿操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderSaga {

    // 核心依赖（从 10 个减少到 5 个）
    private final DistributedLockManager lockManager;
    private final SagaCoordinator sagaCoordinator;
    private final OrderCompensationService compensationService;
    private final OrderCreationExecutor orderCreationExecutor;

    private static final String ORDER_LOCK_PREFIX = "eo:order:lock:product:";

    /**
     * 执行订单创建 Saga
     */
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResult execute(CreateOrderCommand command) {
        List<String> lockKeys = buildLockKeys(command);

        return lockManager.executeWithLocks(lockKeys, 10, () -> doExecute(command));
    }

    /**
     * 构建锁键列表
     */
    private List<String> buildLockKeys(CreateOrderCommand command) {
        return command.getItems().stream()
            .map(CreateOrderCommand.CreateOrderItem::getProductId)
            .distinct()
            .sorted()
            .map(id -> ORDER_LOCK_PREFIX + id)
            .toList();
    }

    /**
     * 执行 Saga 流程
     */
    private CreateOrderResult doExecute(CreateOrderCommand command) {
        SagaStatus sagaStatus = sagaCoordinator.createInitialStatus(command);
        sagaCoordinator.save(sagaStatus);

        List<OrderCompensationService.CompensatingAction> compensations = new ArrayList<>();

        try {
            return executeSagaFlow(command, sagaStatus, compensations);
        } catch (SagaException e) {
            handleSagaFailure(sagaStatus, compensations, e);
            throw new OrderCreationException("订单创建失败：" + e.getMessage(), e);
        } catch (OrderDomainException e) {
            handleSagaFailure(sagaStatus, compensations, e);
            throw new OrderCreationException("订单创建失败：" + e.getMessage(), e);
        } catch (PaymentGatewayAdapterException e) {
            handleSagaFailure(sagaStatus, compensations, e);
            throw new OrderCreationException("订单创建失败：" + e.getMessage(), e);
        } catch (Exception e) {
            log.error("订单创建 Saga 失败 sagaId={} command={}", sagaStatus.sagaId(), command, e);
            handleSagaFailure(sagaStatus, compensations, e);
            throw new OrderCreationException("订单创建失败：" + e.getMessage(), e);
        }
    }

    /**
     * 执行 Saga 流程
     */
    private CreateOrderResult executeSagaFlow(CreateOrderCommand command, SagaStatus sagaStatus,
                                               List<OrderCompensationService.CompensatingAction> compensations) {
        // 步骤 1: 创建订单
        sagaStatus = sagaCoordinator.transitionTo(sagaStatus, SagaState.ORDER_CREATED, "CREATE_ORDER");

        OrderAggregate.OrderCreatedResult createResult = orderCreationExecutor.createOrder(command);
        OrderAggregate aggregate = createResult.aggregate();
        OrderCreatedEvent orderEvent = createResult.event();

        compensations.add(() -> compensationService.cancelOrder(aggregate.id()));

        // 步骤 2: 创建支付
        sagaStatus = sagaCoordinator.transitionTo(sagaStatus, SagaState.PAYMENT_CREATED, "CREATE_PAYMENT");

        orderCreationExecutor.createPayment(orderEvent, command);
        orderCreationExecutor.evictSellerCache(aggregate.sellerId().value());

        // 完成
        sagaCoordinator.transitionTo(sagaStatus, SagaState.COMPLETED, "COMPLETED");

        return new CreateOrderResult(aggregate.id().value(), aggregate.orderNo().value());
    }

    /**
     * 处理 Saga 失败
     */
    private void handleSagaFailure(SagaStatus sagaStatus,
                                    List<OrderCompensationService.CompensatingAction> compensations,
                                    Exception cause) {
        sagaStatus = sagaCoordinator.transitionTo(sagaStatus, SagaState.COMPENSATING, "COMPENSATING");
        sagaStatus = sagaCoordinator.recordError(sagaStatus, cause.getMessage());

        String compensationLog = compensationService.executeCompensations(compensations, cause);

        sagaCoordinator.recordCompensationLog(sagaStatus, compensationLog);
    }

    /**
     * 重试失败的 Saga
     */
    @Transactional(rollbackFor = Exception.class)
    public void retryFailedSaga(String sagaId) {
        SagaStatus sagaStatus = sagaCoordinator.findById(sagaId);

        if (!sagaStatus.canRetry()) {
            throw new OrderDomainException("Saga 不允许重试: " + sagaId);
        }

        try {
            CreateOrderCommand command = sagaCoordinator.deserializePayload(
                sagaId, sagaStatus.payload(), CreateOrderCommand.class
            );

            sagaStatus = sagaCoordinator.incrementRetry(sagaStatus);

            execute(command);
        } catch (SagaSerializationException e) {
            log.error("Saga 重试失败 sagaId={}，反序列化失败", sagaId, e);
            sagaCoordinator.recordError(sagaStatus, e.getMessage());
            throw new OrderDomainException("Saga 重试失败: payload 反序列化错误", e);
        } catch (Exception e) {
            log.error("Saga 重试失败 sagaId={}", sagaId, e);
            sagaCoordinator.recordError(sagaStatus, e.getMessage());
            throw new OrderDomainException("Saga 重试失败", e);
        }
    }
}