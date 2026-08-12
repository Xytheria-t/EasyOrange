package com.cartethyia.easyorange.order.application.command;

import static com.cartethyia.easyorange.order.application.command.CreateOrderCommand.CreateOrderItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.lock.DistributedLockPort;
import com.cartethyia.easyorange.framework.lock.LockAcquisitionException;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.exception.OrderCreationException;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.exception.PaymentGatewayAdapterException;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.order.domain.port.ProductOrderPort;
import com.cartethyia.easyorange.order.domain.port.ProductOrderPort.ProductSnapshot;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderCommandHandler 下单流程测试")
class OrderCommandHandlerCreateTest {

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
    private DistributedLockPort lockPort;

    @Mock
    private ProductQueryPort productQueryPort;

    @Mock
    private IdGenerator idGenerator;

    private OrderCommandHandler commandHandler;

    private static final String BUYER_ID = "1";
    private static final String SELLER_ID = "2";

    @BeforeEach
    void setUp() {
        var preparationService = new OrderPreparation(productOrderPort, productQueryPort, idGenerator);
        commandHandler = new OrderCommandHandler(
                orderRepository,
                eventPublisher,
                orderCachePort,
                lockPort,
                paymentGatewayPort,
                preparationService,
                productOrderPort,
                idGenerator);

        // 默认锁端口正常：直接执行锁内操作（真实行为由 DistributedLockAdapterTest 覆盖）
        when(lockPort.executeWithLocks(anyList(), anyLong(), any()))
                .thenAnswer(inv -> ((DistributedLockPort.LockOperation<?>) inv.getArgument(2)).execute());
        // 详情读源默认返回商品 100 的详情（OrderPreparation 对缺失详情抛错回滚，成功路径必须给出详情）
        when(productQueryPort.getProductsByIds(any()))
                .thenReturn(List.of(new ProductDetail("100", "iPhone 15", new BigDecimal("99.99"), "ONLINE",
                        List.of("img1"), "描述", "A")));
        when(idGenerator.generateId()).thenReturn("018f7c1d-0000-7000-8000-000000000001");
    }

    @Test
    @DisplayName("正常创建订单")
    void createOrder_normalFlow_succeeds() {
        CreateOrderCommand command =
                new CreateOrderCommand(List.of(new CreateOrderItem("100", 1)), "北京市朝阳区", "13800138000", "备注", null);

        ProductSnapshot snapshot = new ProductSnapshot("100", SELLER_ID, new BigDecimal("99.99"), true, 10);
        when(productOrderPort.getSnapshots(any())).thenReturn(List.of(snapshot));
        when(paymentGatewayPort.createPayment(any())).thenReturn("1");

        CreateOrderResult result = commandHandler.handle(BUYER_ID, command);

        assertThat(result).isNotNull();
        assertThat(result.orderNo()).isEqualTo("ORD" + "018f7c1d-0000-7000-8000-000000000001");
        verify(paymentGatewayPort).createPayment(any());
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publish(any());
        verify(productOrderPort).decreaseStock("100", 1);
        verify(lockPort).executeWithLocks(anyList(), anyLong(), any());
    }

    @Test
    @DisplayName("支付失败时抛出 PaymentGatewayAdapterException（单事务回滚兜底，无需反向补偿）")
    void createOrder_paymentFails_throws() {
        CreateOrderCommand command =
                new CreateOrderCommand(List.of(new CreateOrderItem("100", 1)), "北京市朝阳区", "13800138000", null, null);

        ProductSnapshot snapshot = new ProductSnapshot("100", SELLER_ID, new BigDecimal("99.99"), true, 10);
        when(productOrderPort.getSnapshots(any())).thenReturn(List.of(snapshot));
        when(paymentGatewayPort.createPayment(any())).thenThrow(new RuntimeException("支付失败"));

        assertThatThrownBy(() -> commandHandler.handle(BUYER_ID, command))
                .isInstanceOf(PaymentGatewayAdapterException.class)
                .hasMessageContaining("支付失败");
        // 无事务内反向补偿：订单/库存/支付随事务整体回滚
        verify(productOrderPort, never()).restoreStock(anyString(), anyInt());
        verify(orderRepository, never()).update(any(Order.class));
    }

    @Test
    @DisplayName("资产不存在时下单失败")
    void createOrder_productNotFound_throws() {
        CreateOrderCommand command =
                new CreateOrderCommand(List.of(new CreateOrderItem("999", 1)), "北京市朝阳区", "13800138000", null, null);

        when(productOrderPort.getSnapshots(any())).thenReturn(List.of());

        assertThatThrownBy(() -> commandHandler.handle(BUYER_ID, command))
                .isInstanceOf(OrderDomainException.class)
                .hasMessageContaining("资产不存在");
    }

    @Test
    @DisplayName("获取分布式锁失败时抛异常（基础设施异常在用例边界映射为 OrderCreationException）")
    void createOrder_lockFailed_throws() {
        // doThrow 风格：避免 when() 内先调用该方法命中 setUp 的 thenAnswer 桩（其 any() 参数此时为 null）
        doThrow(new LockAcquisitionException("busy"))
                .when(lockPort)
                .executeWithLocks(anyList(), anyLong(), any());

        CreateOrderCommand command =
                new CreateOrderCommand(List.of(new CreateOrderItem("100", 1)), "北京市朝阳区", "13800138000", null, null);

        assertThatThrownBy(() -> commandHandler.handle(BUYER_ID, command))
                .isInstanceOf(OrderCreationException.class)
                .hasMessageContaining("繁忙");
    }
}
