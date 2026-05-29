package com.cartethyia.easyorange.order.application.saga;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.idgen.SnowflakeIdGenerator;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.event.OrderCreatedEvent;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort.ProductSnapshot;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.order.domain.saga.OrderCreationException;
import com.cartethyia.easyorange.order.domain.saga.SagaRepository;
import com.cartethyia.easyorange.order.domain.saga.SagaState;
import com.cartethyia.easyorange.order.domain.saga.SagaStatus;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.ProductId;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderSaga {

    private final OrderRepository orderRepository;
    private final ProductInventoryPort productInventoryPort;
    private final ProductQueryPort productQueryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final DomainEventPublisher eventPublisher;
    private final OrderCachePort<?> orderCachePort;
    private final RedisCache redisCache;
    private final SagaRepository sagaRepository;
    private final ObjectMapper objectMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private static final String ORDER_LOCK_PREFIX = "eo:order:lock:product:";
    private static final String SAGA_TYPE = "CREATE_ORDER";

    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResult execute(CreateOrderCommand command) {
        List<Long> productIds = command.getItems().stream()
                .map(CreateOrderCommand.CreateOrderItem::getProductId)
                .distinct()
                .sorted()
                .toList();

        List<String> lockKeys = productIds.stream()
                .map(id -> ORDER_LOCK_PREFIX + id)
                .toList();

        String lockValue = UUID.randomUUID().toString();

        List<String> acquiredKeys = new ArrayList<>();
        try {
            for (String lockKey : lockKeys) {
                Boolean locked = redisCache.tryLock(lockKey, lockValue, 10, TimeUnit.SECONDS);
                if (!Boolean.TRUE.equals(locked)) {
                    releaseAcquiredLocks(acquiredKeys, lockValue);
                    throw new OrderDomainException("商品下单繁忙，请稍后重试");
                }
                acquiredKeys.add(lockKey);
            }
            return doExecute(command);
        } finally {
            releaseAcquiredLocks(acquiredKeys, lockValue);
        }
    }

    private void releaseAcquiredLocks(List<String> acquiredKeys, String lockValue) {
        for (int i = acquiredKeys.size() - 1; i >= 0; i--) {
            try {
                redisCache.unlockIfValueMatches(acquiredKeys.get(i), lockValue);
            } catch (Exception e) {
                log.warn("释放锁失败 key={}", acquiredKeys.get(i), e);
            }
        }
    }

    private CreateOrderResult doExecute(CreateOrderCommand command) {
        String sagaId = UUID.randomUUID().toString();
        SagaStatus sagaStatus = createInitialSagaStatus(sagaId, command);
        sagaRepository.save(sagaStatus);

        List<CompensatingAction> compensations = new ArrayList<>();

        try {
            sagaStatus = sagaStatus.withState(SagaState.ORDER_CREATED).withStep("CREATE_ORDER");
            sagaRepository.update(sagaStatus);

            OrderAggregate.OrderCreatedResult createResult = createOrder(command);
            OrderAggregate aggregate = createResult.aggregate();
            OrderCreatedEvent orderEvent = createResult.event();
            compensations.add(() -> cancelOrder(aggregate.id()));

            sagaStatus = sagaStatus.withState(SagaState.PAYMENT_CREATED).withStep("CREATE_PAYMENT");
            sagaRepository.update(sagaStatus);

            paymentGatewayPort.createPayment(new PaymentGatewayPort.CreatePaymentRequest(
                    orderEvent.getOrderId(),
                    orderEvent.getTotalAmount(),
                    command.getPaymentMethod() != null ? command.getPaymentMethod() : 1,
                    "ORDER",
                    "订单支付"
            ));

            orderCachePort.evictSellerOrders(aggregate.sellerId().value());

            sagaStatus = sagaStatus.withState(SagaState.COMPLETED).withStep("COMPLETED");
            sagaRepository.update(sagaStatus);

            return new CreateOrderResult(aggregate.id().value(), aggregate.orderNo().value());

        } catch (Exception e) {
            log.error("订单创建 Saga 失败 sagaId={} command={}", sagaId, command, e);

            sagaStatus = sagaStatus.withState(SagaState.COMPENSATING)
                .withStep("COMPENSATING")
                .withError(e.getMessage());
            sagaRepository.update(sagaStatus);

            String compensationLog = compensate(compensations, e);

            sagaStatus = sagaStatus.withState(SagaState.COMPENSATED)
                .withCompensationLog(compensationLog);
            sagaRepository.update(sagaStatus);

            throw new OrderCreationException("订单创建失败：" + e.getMessage(), e);
        }
    }

    private SagaStatus createInitialSagaStatus(String sagaId, CreateOrderCommand command) {
        try {
            String payload = objectMapper.writeValueAsString(command);
            return new SagaStatus(
                sagaId,
                SAGA_TYPE,
                SagaState.PENDING,
                "INIT",
                payload,
                null,
                null,
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("序列化 Saga payload 失败 sagaId={}", sagaId, e);
            return new SagaStatus(
                sagaId,
                SAGA_TYPE,
                SagaState.PENDING,
                "INIT",
                command.getItems().getFirst().getProductId().toString(),
                null,
                null,
                0,
                LocalDateTime.now(),
                LocalDateTime.now()
            );
        }
    }

    private OrderAggregate.OrderCreatedResult createOrder(CreateOrderCommand command) {
        Long buyerId = SecurityContextUtil.getCurrentUserIdOrThrow();

        record ItemPrep(ProductSnapshot snapshot, int quantity) {}
        List<ItemPrep> preps = command.getItems().stream()
                .map(ci -> {
                    ProductSnapshot snapshot = productInventoryPort.getSnapshot(ci.getProductId())
                            .orElseThrow(() -> new OrderDomainException("商品不存在: " + ci.getProductId()));
                    BizRequire.requireTrue(snapshot.isOnline(), "商品已下架: " + ci.getProductId());
                    BizRequire.requireTrue(snapshot.hasStock(), "商品库存不足: " + ci.getProductId());
                    return new ItemPrep(snapshot, ci.getQuantity());
                })
                .toList();

        Long sellerId = preps.getFirst().snapshot().sellerId();
        BizRequire.ne(sellerId, buyerId, "不能购买自己的商品");

        String resolvedAddress = command.getAddress();
        if (resolvedAddress == null || resolvedAddress.isBlank()) {
            resolvedAddress = preps.getFirst().snapshot().location() != null
                    && !preps.getFirst().snapshot().location().isBlank()
                    ? preps.getFirst().snapshot().location()
                    : "未指定";
        }

        Map<Long, ProductDetail> productDetailMap = productQueryPort.getProductsByIds(
                        preps.stream().map(p -> p.snapshot().productId()).toList())
                .stream().collect(Collectors.toMap(ProductDetail::id, d -> d));

        List<OrderItem> items = preps.stream()
                .map(prep -> {
                    Money unitPrice = Money.of(prep.snapshot().price());
                    Money subtotal = unitPrice.multiply(prep.quantity());
                    ProductDetail detail = productDetailMap.get(prep.snapshot().productId());
                    String name = detail != null ? detail.title() : "";
                    String image = (detail != null && detail.images() != null && !detail.images().isEmpty())
                            ? detail.images().getFirst() : "";
                    String description = detail != null && detail.description() != null ? detail.description() : "";
                    String conditionLevel = detail != null && detail.conditionLevel() != null ? detail.conditionLevel() : "";
                    return OrderItem.builder()
                            .id(snowflakeIdGenerator.nextId())
                            .productId(ProductId.of(prep.snapshot().productId()))
                            .snapshot(com.cartethyia.easyorange.order.domain.valueobject.ProductSnapshot.builder()
                                    .productId(prep.snapshot().productId())
                                    .name(name)
                                    .image(image)
                                    .description(description)
                                    .price(unitPrice)
                                    .conditionLevel(conditionLevel)
                                    .build())
                            .unitPrice(unitPrice)
                            .quantity(prep.quantity())
                            .subtotal(subtotal)
                            .build();
                })
                .toList();

        OrderAggregate.OrderCreatedResult result = OrderAggregate.createOrder(
                UserId.of(buyerId),
                UserId.of(sellerId),
                items,
                Address.of(resolvedAddress),
                Phone.of(command.getPhone()),
                command.getRemark(),
                snowflakeIdGenerator.nextId()
        );

        orderRepository.save(result.aggregate());
        eventPublisher.publish(result.event());

        return result;
    }

    @FunctionalInterface
    private interface CompensatingAction {
        void compensate();
    }

    private String compensate(List<CompensatingAction> compensations, Exception cause) {
        log.warn("开始执行补偿逻辑，共 {} 个补偿操作", compensations.size());

        List<String> compensationResults = new ArrayList<>();

        for (int i = compensations.size() - 1; i >= 0; i--) {
            int stepIndex = compensations.size() - i;
            try {
                compensations.get(i).compensate();
                compensationResults.add(String.format("Step %d: SUCCESS", stepIndex));
            } catch (Exception e) {
                compensationResults.add(String.format("Step %d: FAILED - %s", stepIndex, e.getMessage()));
                log.error("补偿操作失败 step={}，将继续执行其他补偿", stepIndex, e);
            }
        }

        log.warn("补偿逻辑执行完成，结果: {}", compensationResults);
        return String.join("; ", compensationResults);
    }

    private void cancelOrder(OrderId orderId) {
        try {
            orderRepository.findById(orderId)
                    .ifPresent(aggregate -> {
                        if (aggregate.canCancel()) {
                            OrderAggregate.OrderCancelledResult result = aggregate.cancel("Saga 补偿取消");
                            orderRepository.update(result.aggregate());
                            orderCachePort.evictOrderCache(aggregate.buyerId().value(), aggregate.sellerId().value());
                        } else {
                            log.warn("Saga: 订单状态不允许取消补偿 orderId={} status={}", orderId.value(), aggregate.status());
                        }
                    });
        } catch (Exception e) {
            log.error("Saga: 订单补偿失败 orderId={}", orderId.value(), e);
            throw new OrderDomainException("订单补偿失败", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void retryFailedSaga(String sagaId) {
        SagaStatus sagaStatus = sagaRepository.findById(sagaId)
            .orElseThrow(() -> new OrderDomainException("Saga 不存在: " + sagaId));

        if (!sagaStatus.canRetry()) {
            throw new OrderDomainException("Saga 不允许重试: " + sagaId);
        }

        try {
            CreateOrderCommand command = objectMapper.readValue(sagaStatus.payload(), CreateOrderCommand.class);

            sagaStatus = sagaStatus.withRetry();
            sagaRepository.update(sagaStatus);

            execute(command);
        } catch (Exception e) {
            log.error("Saga 重试失败 sagaId={}", sagaId, e);
            sagaStatus = sagaStatus.withError(e.getMessage());
            sagaRepository.update(sagaStatus);
            throw new OrderDomainException("Saga 重试失败", e);
        }
    }
}