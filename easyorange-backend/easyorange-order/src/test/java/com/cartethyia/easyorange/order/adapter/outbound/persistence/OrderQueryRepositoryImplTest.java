package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
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
@DisplayName("OrderQueryRepositoryImpl 测试")
class OrderQueryRepositoryImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    private OrderQueryRepositoryImpl orderQueryRepository;

    @BeforeAll
    static void initTableInfo() {
        // 纯 Mockito 环境无 MyBatis-Plus 启动，LambdaQueryWrapper 需要手动初始化实体元数据缓存
        var assistant = new MybatisMapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, OrderDO.class);
        TableInfoHelper.initTableInfo(assistant, OrderItemDO.class);
    }

    @BeforeEach
    void setUp() {
        orderQueryRepository =
                new OrderQueryRepositoryImpl(orderMapper, new OrderDataMapper(new ObjectMapper()), orderItemMapper);
    }

    @Test
    @DisplayName("findPage 批量加载行项，列表读模型携带商品信息")
    void findPage_loadsItemsInBatch() {
        when(orderMapper.selectPage(any(), any())).thenReturn(pageOf(orderDO()));
        when(orderItemMapper.selectList(any())).thenReturn(List.of(itemDO()));

        var page = orderQueryRepository.findPage(new OrderQueryCondition(null, null, null, null, 1, 10));

        assertThat(page.records()).hasSize(1);
        assertThat(page.records().getFirst().items()).hasSize(1);
        assertThat(page.records().getFirst().items().getFirst().productId()).isEqualTo("100");
    }

    @Test
    @DisplayName("findPage 空页不查行项")
    void findPage_emptyPage_returnsEmpty() {
        when(orderMapper.selectPage(any(), any())).thenReturn(pageOf());

        var page = orderQueryRepository.findPage(new OrderQueryCondition(null, null, null, null, 1, 10));

        assertThat(page.records()).isEmpty();
    }

    @Test
    @DisplayName("findById 返回带行项的读模型")
    void findById_loadsItems() {
        when(orderMapper.selectById("1")).thenReturn(orderDO());
        when(orderItemMapper.selectList(any())).thenReturn(List.of(itemDO()));

        OrderReadModel readModel = orderQueryRepository
                .findById(OrderId.of("1"))
                .orElseThrow();

        assertThat(readModel.items()).hasSize(1);
        assertThat(readModel.items().getFirst().productId()).isEqualTo("100");
    }

    private Page<OrderDO> pageOf(OrderDO... records) {
        Page<OrderDO> page = new Page<>(1, 10);
        page.setRecords(List.of(records));
        page.setTotal(records.length);
        return page;
    }

    private OrderDO orderDO() {
        return OrderDO.builder()
                .id("1")
                .orderNo("ORD1")
                .buyerId("1")
                .sellerId("2")
                .totalAmount(new java.math.BigDecimal("99.99"))
                .status(OrderStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.UNPAID)
                .address("地址")
                .phone("13800138000")
                .remark("备注")
                .version(0)
                .build();
    }

    private OrderItemDO itemDO() {
        return OrderItemDO.builder()
                .id("1")
                .orderId("1")
                .productId("100")
                .productSnapshot("null")
                .unitPrice(new java.math.BigDecimal("99.99"))
                .quantity(1)
                .subtotal(new java.math.BigDecimal("99.99"))
                .build();
    }
}
