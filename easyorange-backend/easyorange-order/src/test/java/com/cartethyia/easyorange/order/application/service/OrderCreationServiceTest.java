package com.cartethyia.easyorange.order.application.service;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.port.ProductOrderPort;
import com.cartethyia.easyorange.order.domain.port.ProductOrderPort.ProductSnapshot;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.exception.OrderCreationException;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderCreationService 下单流程测试")
class OrderCreationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductOrderPort productOrderPort;

    @Mock
    private PaymentGatewayPort paymentGatewayPort;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private OrderCachePort<OrderVO> orderCachePort;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private ProductQueryPort productQueryPort;

    @Mock
    private IdGenerator idGenerator;

    private OrderCreationService service;

    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";

    @BeforeEach
    void setUp() throws InterruptedException {
        var lockManager = new DistributedLockManager(redissonClient);
        var preparationService = new OrderPreparationService(productOrderPort, productQueryPort, idGenerator);
        var orderCreationExecutor = new OrderCreationExecutor(
            orderRepository, eventPublisher, paymentGatewayPort, orderCachePort, preparationService, idGenerator
        );
        service = new OrderCreationService(lockManager, orderCreationExecutor, productOrderPort);

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
    @DisplayName("正常创建订单")
    void execute_normalFlow_succeeds() {
        CreateOrderCommand command = new CreateOrderCommand(
                List.of(new CreateOrderItem("100", 1)),
                "北京市朝阳区", "13800138000", "备注", null
        );

        ProductSnapshot snapshot = new ProductSnapshot("100", SELLER_ID, new BigDecimal("99.99"), true, 10, "北京");
        when(productOrderPort.getSnapshot("100")).thenReturn(Optional.of(snapshot));
        when(paymentGatewayPort.createPayment(any())).thenReturn("1");

        CreateOrderResult result = service.execute(command);

        assertThat(result).isNotNull();
        verify(paymentGatewayPort).createPayment(any());
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publish(any());
        verify(productOrderPort).decreaseStock("100", 1);
        verify(lock, atLeastOnce()).unlock();
    }

    @Test
    @DisplayName("支付失败时抛出 OrderCreationException（单事务回滚兜底，无需反向补偿）")
    void execute_paymentFails_throws() {
        CreateOrderCommand command = new CreateOrderCommand(
                List.of(new CreateOrderItem("100", 1)),
                "北京市朝阳区", "13800138000", null, null
        );

        ProductSnapshot snapshot = new ProductSnapshot("100", SELLER_ID, new BigDecimal("99.99"), true, 10, "北京");
        when(productOrderPort.getSnapshot("100")).thenReturn(Optional.of(snapshot));
        when(paymentGatewayPort.createPayment(any())).thenThrow(new RuntimeException("支付失败"));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(OrderCreationException.class)
                .hasMessageContaining("支付失败");
        // 无事务内反向补偿：订单/库存/支付随事务整体回滚
        verify(productOrderPort, never()).restoreStock(anyString(), anyInt());
        verify(orderRepository, never()).update(any(Order.class));
    }

    @Test
    @DisplayName("资产不存在时下单失败")
    void execute_productNotFound_throws() {
        CreateOrderCommand command = new CreateOrderCommand(
                List.of(new CreateOrderItem("999", 1)),
                "北京市朝阳区", "13800138000", null, null
        );

        when(productOrderPort.getSnapshot("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(OrderCreationException.class)
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

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(OrderCreationException.class)
                .hasMessageContaining("繁忙");
    }
}
