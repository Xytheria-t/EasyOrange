package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.domain.port.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderReadRepositoryImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    private final OrderDataMapper dataMapper = new OrderDataMapper(new ObjectMapper());

    private OrderReadRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new OrderReadRepositoryImpl(orderMapper, dataMapper, orderItemMapper);
    }

    @Test
    @DisplayName("findPage 批加载明细，列表 ReadModel 携带 items（防列表商品信息为空）")
    void findPage_attachesBatchedItems() {
        String orderId = "order-1";
        var order = OrderDO.builder().id(orderId).orderNo("ORD001").build();
        var item = OrderItemDO.builder()
                .id("item-1")
                .orderId(orderId)
                .productId("p-1")
                .unitPrice(new BigDecimal("10.00"))
                .quantity(1)
                .subtotal(new BigDecimal("10.00"))
                .build();
        Page<OrderDO> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(order));

        when(orderMapper.selectPage(any(), any())).thenReturn(page);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        PageResult<OrderReadModel> result = repository.findPage(new OrderQueryCondition(null, null, null, null, 1, 10));

        assertThat(result.records()).hasSize(1);
        OrderReadModel first = result.records().getFirst();
        assertThat(first.id()).isEqualTo(orderId);
        assertThat(first.items()).hasSize(1);
        assertThat(first.items().getFirst().productId()).isEqualTo("p-1");
    }

    @Test
    @DisplayName("findPage 空页不查明细，直接返回空")
    void findPage_emptyPage_doesNotQueryItems() {
        Page<OrderDO> page = new Page<>(1, 10, 0);
        page.setRecords(List.of());
        when(orderMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<OrderReadModel> result = repository.findPage(new OrderQueryCondition(null, null, null, null, 1, 10));

        assertThat(result.records()).isEmpty();
    }
}
