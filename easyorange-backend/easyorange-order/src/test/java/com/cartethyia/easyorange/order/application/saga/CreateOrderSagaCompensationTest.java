package com.cartethyia.easyorange.order.application.saga;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.application.saga.support.DistributedLockManager;
import com.cartethyia.easyorange.order.application.saga.support.OrderCompensationService;
import com.cartethyia.easyorange.order.application.saga.support.OrderCreationExecutor;
import com.cartethyia.easyorange.order.application.saga.support.OrderPreparationService;
import com.cartethyia.easyorange.order.application.saga.support.SagaCoordinator;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.aggregate.OrderReconstructSpec;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort.ProductSnapshot;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.saga.OrderCreationException;
import com.cartethyia.easyorange.order.domain.saga.SagaRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.cartethyia.easyorange.order.application.command.CreateOrderCommand.CreateOrderItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CreateOrderSaga 补偿测试")
class CreateOrderSagaCompensationTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductInventoryPort productInventoryPort;

    @Mock
    private PaymentGatewayPort paymentGatewayPort;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private OrderCachePort orderCachePort;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private SagaRepository sagaRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProductQueryPort productQueryPort;

    @Mock
    private IdGenerator idGenerator;

    private CreateOrderSaga saga;

    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";

    @BeforeEach
    void setUp() throws InterruptedException {
        // Construct support classes with mocks
        var lockManager = new DistributedLockManager(redissonClient);
        var sagaCoordinator = new SagaCoordinator(sagaRepository, objectMapper);
        var compensationService = new OrderCompensationService(orderRepository, orderCachePort);
        var preparationService = new OrderPreparationService(productInventoryPort, productQueryPort, idGenerator);
        var orderCreationExecutor = new OrderCreationExecutor(
            orderRepository, eventPublisher, paymentGatewayPort, orderCachePort, preparationService, idGenerator
        );
        saga = new CreateOrderSaga(lockManager, sagaCoordinator, compensationService, orderCreationExecutor);

        Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                BUYER_ID, null, authorities
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(productQueryPort.getProductsByIds(any())).thenReturn(List.of());
        when(idGenerator.generateId()).thenReturn("ORD100");
    }

    @Test
    @DisplayName("正常创建订单 Saga 成功")
    void execute_normalFlow_succeeds() {
        CreateOrderCommand command = new CreateOrderCommand(
                List.of(new CreateOrderItem("100", 1)),
                "北京市朝阳区", "13800138000", "备注", null
        );

        ProductSnapshot snapshot = new ProductSnapshot("100", SELLER_ID, new BigDecimal("99.99"), true, true, "北京");
        when(productInventoryPort.getSnapshot("100")).thenReturn(Optional.of(snapshot));
        when(paymentGatewayPort.createPayment(any())).thenReturn("1");

        CreateOrderResult result = saga.execute(command);

        assertThat(result).isNotNull();
        verify(paymentGatewayPort).createPayment(any());
        verify(orderRepository).save(any(OrderAggregate.class));
        verify(eventPublisher).publish(any());
        verify(lock, atLeastOnce()).unlock();
    }

    @Test
    @DisplayName("支付失败时执行订单补偿")
    void execute_paymentFails_cancelsOrder() {
        CreateOrderCommand command = new CreateOrderCommand(
                List.of(new CreateOrderItem("100", 1)),
                "北京市朝阳区", "13800138000", null, null
        );

        ProductSnapshot snapshot = new ProductSnapshot("100", SELLER_ID, new BigDecimal("99.99"), true, true, "北京");
        when(productInventoryPort.getSnapshot("100")).thenReturn(Optional.of(snapshot));
        when(paymentGatewayPort.createPayment(any())).thenThrow(new RuntimeException("支付失败"));

        OrderAggregate cancelledAggregate = OrderAggregate.from(
                new OrderReconstructSpec(
                        OrderId.of("1"), OrderNo.of("ORD1"),
                        UserId.of(BUYER_ID), UserId.of(SELLER_ID),
                        List.of(),
                        Money.of(new BigDecimal("99.99")),
                        OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID,
                        Address.of("地址"), Phone.of("13800138000"),
                        "备注", null, null
                )
        );
        when(orderRepository.findById(any(OrderId.class))).thenReturn(Optional.of(cancelledAggregate));

        assertThatThrownBy(() -> saga.execute(command))
                .isInstanceOf(OrderCreationException.class);

        verify(orderRepository).update(any(OrderAggregate.class));
    }

    @Test
    @DisplayName("资产不存在时 Saga 失败")
    void execute_productNotFound_throws() {
        CreateOrderCommand command = new CreateOrderCommand(
                List.of(new CreateOrderItem("999", 1)),
                "北京市朝阳区", "13800138000", null, null
        );

        when(productInventoryPort.getSnapshot("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saga.execute(command))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("资产不存在");
    }

    @Test
    @DisplayName("获取分布式锁失败时抛异常")
    void execute_lockFailed_throws() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        CreateOrderCommand command = new CreateOrderCommand(
                List.of(new CreateOrderItem("100", 1)),
                "北京市朝阳区", "13800138000", null, null
        );

        assertThatThrownBy(() -> saga.execute(command))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("繁忙");
    }
}