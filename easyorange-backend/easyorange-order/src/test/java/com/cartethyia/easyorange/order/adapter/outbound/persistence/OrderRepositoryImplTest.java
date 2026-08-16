package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import static com.cartethyia.easyorange.order.domain.aggregate.OrderTestFixture.AMOUNT;
import static com.cartethyia.easyorange.order.domain.aggregate.OrderTestFixture.BUYER_ID;
import static com.cartethyia.easyorange.order.domain.aggregate.OrderTestFixture.PRODUCT_ID;
import static com.cartethyia.easyorange.order.domain.aggregate.OrderTestFixture.SELLER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.aggregate.OrderTestFixture;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderRepositoryImpl 测试")
class OrderRepositoryImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    private OrderRepositoryImpl orderRepository;

    @BeforeAll
    static void initTableInfo() {
        // 纯 Mockito 环境无 MyBatis-Plus 启动，LambdaQueryWrapper 需要手动初始化实体元数据缓存
        var assistant = new MybatisMapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, OrderDO.class);
        TableInfoHelper.initTableInfo(assistant, OrderItemDO.class);
    }

    @BeforeEach
    void setUp() {
        orderRepository =
                new OrderRepositoryImpl(orderMapper, new OrderDataMapper(new ObjectMapper()), orderItemMapper);
    }

    @Test
    @DisplayName("save 级联插入订单项（订单项 id 由领域层生成）")
    void save_cascadesItemsWithOrderId() {
        var order = OrderTestFixture.pendingPaymentOrder();

        orderRepository.save(order);

        verify(orderMapper).insert(argThat((OrderDO o) -> o.getId().equals("1")));
        verify(orderItemMapper)
                .batchInsert(argThat(items -> items.size() == 1
                        && items.getFirst().getOrderId().equals("1")
                        && items.getFirst().getId().equals("1")));
    }

    @Test
    @DisplayName("update 只更新订单主表，不触碰订单行（行项创建后不可变快照）")
    void update_doesNotTouchItems() {
        var order = OrderTestFixture.pendingPaymentOrder();

        orderRepository.update(order);

        verify(orderMapper).updateById(any(OrderDO.class));
        verify(orderItemMapper, never()).deleteByOrderId(anyString());
        verify(orderItemMapper, never()).batchInsert(anyList());
    }

    @Test
    @DisplayName("findExpiredOrders 批量加载行项，取消事件可提取商品 ID（缺行项会导致库存永不恢复）")
    void findExpiredOrders_loadsItems() {
        when(orderMapper.selectList(any())).thenReturn(List.of(pendingPaymentOrderDO()));
        when(orderItemMapper.selectList(any())).thenReturn(List.of(itemDO()));

        List<Order> orders = orderRepository.findExpiredOrders(30);

        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().items()).hasSize(1);
        var transition = orders.getFirst().cancel("超时", LocalDateTime.now());
        assertThat(transition.event().productIds()).containsExactly(PRODUCT_ID);
    }

    @Test
    @DisplayName("findShippedOrdersBefore 批量加载行项，完成事件可提取商品 ID（缺行项会导致商品漏标记售出）")
    void findShippedOrdersBefore_loadsItems() {
        when(orderMapper.selectList(any())).thenReturn(List.of(shippedOrderDO()));
        when(orderItemMapper.selectList(any())).thenReturn(List.of(itemDO()));

        List<Order> orders = orderRepository.findShippedOrdersBefore(LocalDateTime.now().minusDays(7));

        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().items()).hasSize(1);
        var transition = orders.getFirst().confirmReceipt(LocalDateTime.now());
        assertThat(transition.event().productIds()).containsExactly(PRODUCT_ID);
    }

    private OrderDO pendingPaymentOrderDO() {
        return orderDO(OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID);
    }

    private OrderDO shippedOrderDO() {
        return orderDO(OrderStatus.SHIPPED, PaymentStatus.PAID);
    }

    private OrderDO orderDO(OrderStatus status, PaymentStatus paymentStatus) {
        return OrderDO.builder()
                .id("1")
                .orderNo("ORD1")
                .buyerId(BUYER_ID)
                .sellerId(SELLER_ID)
                .totalAmount(AMOUNT)
                .status(status)
                .paymentStatus(paymentStatus)
                .address("地址")
                .phone("13800138000")
                .remark("备注")
                .build();
    }

    private OrderItemDO itemDO() {
        return OrderItemDO.builder()
                .id("1")
                .orderId("1")
                .productId(PRODUCT_ID)
                .productSnapshot("null")
                .unitPrice(AMOUNT)
                .quantity(1)
                .subtotal(AMOUNT)
                .build();
    }
}
