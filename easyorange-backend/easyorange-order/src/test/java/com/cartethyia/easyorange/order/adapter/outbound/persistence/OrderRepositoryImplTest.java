package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.cartethyia.easyorange.order.domain.aggregate.OrderTestFixture;
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
    @DisplayName("update 物理删除旧订单项后重插，避免逻辑删除行累积")
    void update_physicallyReplacesItems() {
        var order = OrderTestFixture.pendingPaymentOrder();

        orderRepository.update(order);

        verify(orderMapper).updateById(any(OrderDO.class));
        verify(orderItemMapper).deleteByOrderId("1");
        verify(orderItemMapper)
                .batchInsert(argThat(items ->
                        items.size() == 1 && items.getFirst().getOrderId().equals("1")));
    }
}
