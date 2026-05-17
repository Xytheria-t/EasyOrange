package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.admin.dto.request.AdminOrderQueryRequest;
import com.cartethyia.easyorange.admin.dto.response.AdminOrderDetailVO;
import com.cartethyia.easyorange.admin.dto.response.AdminOrderVO;
import com.cartethyia.easyorange.admin.dto.response.OrderStatsVO;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderDO;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderMapper;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.output.OrderReadRepository;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOrderService 单元测试")
class AdminOrderServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderReadRepository orderReadRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private AdminOrderService orderService;

    private static final Long ORDER_ID = 100L;
    private static final Long BUYER_ID = 1L;
    private static final Long SELLER_ID = 2L;
    private static final Long PRODUCT_ID = 200L;

    private OrderDO createOrderDO(int status) {
        OrderDO order = new OrderDO();
        order.setId(ORDER_ID);
        order.setOrderNo("ORD2026001");
        order.setBuyerId(BUYER_ID);
        order.setSellerId(SELLER_ID);
        order.setProductId(PRODUCT_ID);
        order.setAmount(new BigDecimal("99.99"));
        order.setStatus(status);
        order.setPaymentStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setDelFlag(0);
        return order;
    }

    private OrderReadModel createReadModel(int status) {
        return new OrderReadModel(
                ORDER_ID, "ORD2026001", BUYER_ID, SELLER_ID, PRODUCT_ID,
                new BigDecimal("99.99"), status, "待支付", 0,
                "地址", "13800138000", "备注", null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("listOrders")
    class ListOrdersTests {

        @Test
        @DisplayName("分页查询订单列表")
        void listOrders_returnsPage() {
            AdminOrderQueryRequest request = new AdminOrderQueryRequest();
            OrderDO order = createOrderDO(0);

            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(invocation -> {
                        Page<OrderDO> p = invocation.getArgument(0);
                        p.setRecords(List.of(order));
                        p.setTotal(1);
                        return p;
                    });
            UserEntity buyer = UserEntity.builder().id(BUYER_ID).nickName("买家").build();
            UserEntity seller = UserEntity.builder().id(SELLER_ID).nickName("卖家").build();
            when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(buyer, seller));
            ProductDO orderTestProduct = ProductDO.builder().id(PRODUCT_ID).name("测试商品").price(new BigDecimal("99.99")).build();
            orderTestProduct.setDelFlag(0);
            when(productMapper.selectBatchIds(anyCollection())).thenReturn(List.of(orderTestProduct));

            PageResult<AdminOrderVO> result = orderService.listOrders(request);

            assertThat(result.records()).hasSize(1);
            assertThat(result.total()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getOrderDetail")
    class GetOrderDetailTests {

        @Test
        @DisplayName("获取订单详情成功")
        void getOrderDetail_success() {
            OrderReadModel model = createReadModel(0);
            when(orderReadRepository.findById(new OrderId(ORDER_ID))).thenReturn(Optional.of(model));
            when(userMapper.selectById(BUYER_ID)).thenReturn(
                    UserEntity.builder().id(BUYER_ID).nickName("买家").build());
            when(userMapper.selectById(SELLER_ID)).thenReturn(
                    UserEntity.builder().id(SELLER_ID).nickName("卖家").build());
            when(productMapper.selectById(PRODUCT_ID)).thenReturn(
                    ProductDO.builder().id(PRODUCT_ID).name("测试商品").price(new BigDecimal("99.99")).build());

            AdminOrderDetailVO detail = orderService.getOrderDetail(ORDER_ID);

            assertThat(detail).isNotNull();
            assertThat(detail.orderId()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("订单不存在时抛出异常")
        void getOrderDetail_notFound_throws() {
            when(orderReadRepository.findById(new OrderId(ORDER_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderDetail(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单不存在");
        }
    }

    @Nested
    @DisplayName("getOrderStats / cancelOrder / forceComplete / refundOrder")
    class OrderOperationsTests {

        @Test
        @DisplayName("获取订单统计")
        void getOrderStats_returnsStats() {
            when(orderReadRepository.countByStatus(null)).thenReturn(100L);
            when(orderReadRepository.countByStatus(OrderStatus.PENDING_PAYMENT.getCode())).thenReturn(20L);
            when(orderReadRepository.countByStatus(OrderStatus.PAID.getCode())).thenReturn(30L);
            when(orderReadRepository.countByStatus(OrderStatus.SHIPPED.getCode())).thenReturn(15L);
            when(orderReadRepository.countByStatus(OrderStatus.COMPLETED.getCode())).thenReturn(25L);
            when(orderReadRepository.countByStatus(OrderStatus.CANCELLED.getCode())).thenReturn(5L);
            when(orderReadRepository.countByStatus(OrderStatus.REFUNDED.getCode())).thenReturn(5L);
            when(orderMapper.selectCount(any())).thenReturn(10L);

            OrderStatsVO stats = orderService.getOrderStats();

            assertThat(stats.totalOrders()).isEqualTo(100);
            assertThat(stats.pendingPayment()).isEqualTo(20);
            assertThat(stats.toShip()).isEqualTo(30);
            assertThat(stats.completed()).isEqualTo(25);
        }

        @Test
        @DisplayName("取消订单成功")
        void cancelOrder_success() {
            OrderDO order = createOrderDO(0);
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            orderService.cancelOrder(ORDER_ID, "买家申请取消");

            verify(orderMapper).updateById(order);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED.getCode());
        }

        @Test
        @DisplayName("取消不存在的订单抛出异常")
        void cancelOrder_notFound_throws() {
            when(orderMapper.selectById(ORDER_ID)).thenReturn(null);

            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, "取消"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("订单不存在");
        }

        @Test
        @DisplayName("强制完成订单成功")
        void forceComplete_success() {
            OrderDO order = createOrderDO(2);
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            orderService.forceComplete(ORDER_ID, "强制完成");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED.getCode());
        }

        @Test
        @DisplayName("退款订单成功")
        void refundOrder_success() {
            OrderDO order = createOrderDO(1);
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            orderService.refundOrder(ORDER_ID, "商品问题退款");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED.getCode());
        }

        @Test
        @DisplayName("已取消订单无法退款")
        void refundOrder_cancelled_throws() {
            OrderDO order = createOrderDO(4);
            when(orderMapper.selectById(ORDER_ID)).thenReturn(order);

            assertThatThrownBy(() -> orderService.refundOrder(ORDER_ID, "退款"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无法退款");
        }
    }
}
