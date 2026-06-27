package com.cartethyia.easyorange.order.application.saga;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.idgen.SnowflakeIdGenerator;
import com.cartethyia.easyorange.framework.cache.RedisCache;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.application.saga.support.DistributedLockManager;
import com.cartethyia.easyorange.order.application.saga.support.OrderCompensationService;
import com.cartethyia.easyorange.order.application.saga.support.OrderCreationExecutor;
import com.cartethyia.easyorange.order.application.saga.support.OrderPreparationService;
import com.cartethyia.easyorange.order.application.saga.support.SagaCoordinator;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort;
import com.cartethyia.easyorange.order.domain.port.ProductInventoryPort.ProductSnapshot;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
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
    private RedisCache redisCache;

    @Mock
    private SagaRepository sagaRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProductQueryPort productQueryPort;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    private CreateOrderSaga saga;

    private static final Long BUYER_ID = 1L;
    private static final Long SELLER_ID = 2L;

    @BeforeEach
    void setUp() {
        // Construct support classes with mocks
        var lockManager = new DistributedLockManager(redisCache);
        var sagaCoordinator = new SagaCoordinator(sagaRepository, objectMapper);
        var compensationService = new OrderCompensationService(orderRepository, orderCachePort);
        var preparationService = new OrderPreparationService(productInventoryPort, productQueryPort, snowflakeIdGenerator);
        var orderCreationExecutor = new OrderCreationExecutor(
            orderRepository, eventPublisher, paymentGatewayPort, orderCachePort, preparationService, snowflakeIdGenerator
        );
        saga = new CreateOrderSaga(lockManager, sagaCoordinator, compensationService, orderCreationExecutor);

        Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                BUYER_ID, null, authorities
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(redisCache.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(productQueryPort.getProductsByIds(any())).thenReturn(List.of());
        when(snowflakeIdGenerator.nextId()).thenReturn(100L);
    }

    @Test
    @DisplayName("正常创建订单 Saga 成功")
    void execute_normalFlow_succeeds() {
        CreateOrderCommand command = new CreateOrderCommand();
        command.setItems(List.of(new CreateOrderItem(100L, 1)));
        command.setAddress("北京市朝阳区");
        command.setPhone("13800138000");
        command.setRemark("备注");

        ProductSnapshot snapshot = new ProductSnapshot(100L, SELLER_ID, new BigDecimal("99.99"), true, true, "北京");
        when(productInventoryPort.getSnapshot(100L)).thenReturn(Optional.of(snapshot));
        when(paymentGatewayPort.createPayment(any())).thenReturn(1L);

        CreateOrderResult result = saga.execute(command);

        assertThat(result).isNotNull();
        verify(paymentGatewayPort).createPayment(any());
        verify(orderRepository).save(any(OrderAggregate.class));
        verify(eventPublisher).publish(any());
        verify(redisCache).unlock(anyString(), anyString());
    }

    @Test
    @DisplayName("支付失败时执行订单补偿")
    void execute_paymentFails_cancelsOrder() {
        CreateOrderCommand command = new CreateOrderCommand();
        command.setItems(List.of(new CreateOrderItem(100L, 1)));
        command.setAddress("北京市朝阳区");
        command.setPhone("13800138000");

        ProductSnapshot snapshot = new ProductSnapshot(100L, SELLER_ID, new BigDecimal("99.99"), true, true, "北京");
        when(productInventoryPort.getSnapshot(100L)).thenReturn(Optional.of(snapshot));
        when(paymentGatewayPort.createPayment(any())).thenThrow(new RuntimeException("支付失败"));

        OrderAggregate cancelledAggregate = OrderAggregate.fromRaw(
                1L, "ORD1", BUYER_ID, SELLER_ID,
                new BigDecimal("99.99"), OrderStatus.PENDING_PAYMENT.getCode(), 0,
                "地址", "13800138000", "备注", null, null
        );
        when(orderRepository.findById(any(OrderId.class))).thenReturn(Optional.of(cancelledAggregate));

        assertThatThrownBy(() -> saga.execute(command))
                .isInstanceOf(OrderCreationException.class);

        verify(orderRepository).update(any(OrderAggregate.class));
    }

    @Test
    @DisplayName("资产不存在时 Saga 失败")
    void execute_productNotFound_throws() {
        CreateOrderCommand command = new CreateOrderCommand();
        command.setItems(List.of(new CreateOrderItem(999L, 1)));
        command.setAddress("北京市朝阳区");
        command.setPhone("13800138000");

        when(productInventoryPort.getSnapshot(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saga.execute(command))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("资产不存在");
    }

    @Test
    @DisplayName("获取分布式锁失败时抛异常")
    void execute_lockFailed_throws() {
        when(redisCache.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        CreateOrderCommand command = new CreateOrderCommand();
        command.setItems(List.of(new CreateOrderItem(100L, 1)));
        command.setAddress("北京市朝阳区");
        command.setPhone("13800138000");

        assertThatThrownBy(() -> saga.execute(command))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("繁忙");
    }
}