package com.cartethyia.easyorange.order.application.query;

import static com.cartethyia.easyorange.order.domain.constant.OrderStatus.PENDING_PAYMENT;
import static com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus.UNPAID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.application.query.assembler.OrderReadModelAssembler;
import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.port.OrderCachePort;
import com.cartethyia.easyorange.order.domain.port.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort;
import com.cartethyia.easyorange.order.domain.port.ProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderQueryHandler 单元测试")
@SuppressWarnings("unchecked")
class OrderQueryHandlerTest {

    private static final String BUYER_ID = "100";
    private static final String SELLER_ID = "200";

    @Mock
    private OrderReadRepository orderReadRepository;

    @Mock
    private ProductQueryPort productQueryPort;

    @Mock
    private OrderCachePort orderCachePort;

    @Mock
    private OrderReadModelAssembler readModelAssembler;

    private OrderQueryHandler handler;

    private OrderReadModel testOrderReadModel;
    private OrderVO mockOrderVO;

    @BeforeEach
    void setUp() {
        handler = new OrderQueryHandler(orderReadRepository, productQueryPort, orderCachePort, readModelAssembler);

        testOrderReadModel = new OrderReadModel(
                "1",
                "ORD001",
                BUYER_ID,
                SELLER_ID,
                List.of(),
                new BigDecimal("99.99"),
                PENDING_PAYMENT.getCode(),
                "待付款",
                UNPAID.getCode(),
                "北京市朝阳区",
                "13800138000",
                "备注",
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now());

        mockOrderVO = OrderVO.builder()
                .id("1")
                .orderNo("ORD001")
                .totalAmount(new BigDecimal("99.99"))
                .build();

        when(readModelAssembler.toOrderVO(any(OrderReadModel.class), anyMap(), anyBoolean()))
                .thenReturn(mockOrderVO);
        when(readModelAssembler.toOrderVOs(any(), anyMap())).thenReturn(List.of(mockOrderVO));
        when(orderCachePort.buildOrderListKey(any(), any(), any(), any())).thenReturn("eo:order:list:key");
        when(orderCachePort.getOrderList(any())).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    @Nested
    @DisplayName("getOrderDetailForOwner")
    class GetOrderDetailForOwnerTest {

        @Test
        @DisplayName("订单不存在时抛出 ORDER_NOT_FOUND")
        void nonExistingOrder_throwsNotFound() {
            when(orderReadRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> handler.getOrderDetailForOwner("999"))
                    .isInstanceOf(OrderDomainException.class)
                    .extracting("code")
                    .isEqualTo("B3001");
        }

        @Test
        @DisplayName("非订单所有方访问抛出 ORDER_NOT_OWNER")
        void nonOwner_throwsNotOwner() {
            authenticateAs("999");
            when(orderReadRepository.findById(any())).thenReturn(Optional.of(testOrderReadModel));

            assertThatThrownBy(() -> handler.getOrderDetailForOwner("1"))
                    .isInstanceOf(OrderDomainException.class)
                    .extracting("code")
                    .isEqualTo("B3003");
        }

        @Test
        @DisplayName("买方访问自己的订单返回详情且不脱敏")
        void buyerOwner_returnsVOWithoutMask() {
            authenticateAs(BUYER_ID);
            when(orderReadRepository.findById(any())).thenReturn(Optional.of(testOrderReadModel));
            when(productQueryPort.getProductsByIds(any())).thenReturn(List.of(testProductDetail()));

            OrderVO result = handler.getOrderDetailForOwner("1");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("1");
            assertThat(result.getOrderNo()).isEqualTo("ORD001");
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
            verify(readModelAssembler).toOrderVO(eq(testOrderReadModel), anyMap(), eq(false));
        }

        @Test
        @DisplayName("卖方访问自己的订单返回详情")
        void sellerOwner_returnsVO() {
            authenticateAs(SELLER_ID);
            when(orderReadRepository.findById(any())).thenReturn(Optional.of(testOrderReadModel));
            when(productQueryPort.getProductsByIds(any())).thenReturn(List.of(testProductDetail()));

            OrderVO result = handler.getOrderDetailForOwner("1");

            assertThat(result).isNotNull();
            verify(readModelAssembler).toOrderVO(eq(testOrderReadModel), anyMap(), eq(false));
        }
    }

    @Nested
    @DisplayName("listOrders")
    class ListOrdersTest {

        @Test
        @DisplayName("分页查询返回结果")
        void pageQuery_returnsPageResult() {
            PageResult<OrderReadModel> pageResult = PageResult.of(List.of(testOrderReadModel), 1L, 1, 10);
            when(orderReadRepository.findPage(any(OrderQueryCondition.class))).thenReturn(pageResult);
            when(productQueryPort.getProductsByIds(any())).thenReturn(List.of(testProductDetail()));

            PageResult<OrderVO> result = handler.listOrders(new OrderListQuery(null, null, null, null, 1, 10));

            assertThat(result).isNotNull();
            assertThat(result.records()).hasSize(1);
            assertThat(result.records().getFirst().getOrderNo()).isEqualTo("ORD001");
        }

        @Test
        @DisplayName("空结果返回空列表")
        void emptyResult_returnsEmptyPage() {
            PageResult<OrderReadModel> emptyPage = PageResult.of(List.of(), 0L, 1, 10);
            when(orderReadRepository.findPage(any(OrderQueryCondition.class))).thenReturn(emptyPage);

            PageResult<OrderVO> result = handler.listOrders(new OrderListQuery(null, null, null, null, 1, 10));

            assertThat(result).isNotNull();
            assertThat(result.records()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMyOrders / getSoldOrders 缓存")
    class CachedOrderListTest {

        @Test
        @DisplayName("缓存命中时直接返回缓存，不查仓储")
        void myOrders_cacheHit_returnsCachedWithoutQueryingRepository() {
            authenticateAs(BUYER_ID);
            PageResult<OrderVO> cached = PageResult.of(List.of(mockOrderVO), 1L, 1, 10);
            when(orderCachePort.getOrderList(any())).thenReturn(Optional.of(cached));

            PageResult<OrderVO> result = handler.getMyOrders(new OrderListQuery(null, null, null, null, 1, 10));

            assertThat(result).isSameAs(cached);
            verify(orderReadRepository, never()).findPage(any());
        }

        @Test
        @DisplayName("缓存未命中时查询仓储并写入缓存")
        void myOrders_cacheMiss_queriesAndPuts() {
            authenticateAs(BUYER_ID);
            PageResult<OrderReadModel> pageResult = PageResult.of(List.of(testOrderReadModel), 1L, 1, 10);
            when(orderReadRepository.findPage(any(OrderQueryCondition.class))).thenReturn(pageResult);
            when(productQueryPort.getProductsByIds(any())).thenReturn(List.of(testProductDetail()));

            handler.getMyOrders(new OrderListQuery(null, null, null, null, 1, 10));

            verify(orderReadRepository).findPage(any(OrderQueryCondition.class));
            verify(orderCachePort).putOrderList("eo:order:list:key", PageResult.of(List.of(mockOrderVO), 1L, 1, 10));
        }

        @Test
        @DisplayName("缓存 key 包含分页参数，避免跨页污染")
        void myOrders_pageTwo_usesPaginationInCacheKey() {
            authenticateAs(BUYER_ID);
            when(orderReadRepository.findPage(any(OrderQueryCondition.class)))
                    .thenReturn(PageResult.of(List.of(testOrderReadModel), 1L, 2, 10));

            handler.getMyOrders(new OrderListQuery(null, null, null, null, 2, 10));

            verify(orderCachePort).buildOrderListKey(eq(BUYER_ID), any(), eq(2), eq(10));
        }

        @Test
        @DisplayName("orderNo 过滤时绕过缓存直接查询")
        void soldOrders_withOrderNoFilter_skipsCache() {
            authenticateAs(SELLER_ID);
            when(orderReadRepository.findPage(any(OrderQueryCondition.class)))
                    .thenReturn(PageResult.of(List.of(testOrderReadModel), 1L, 1, 10));
            when(productQueryPort.getProductsByIds(any())).thenReturn(List.of(testProductDetail()));

            handler.getSoldOrders(new OrderListQuery("ORD001", null, null, null, 1, 10));

            verify(orderCachePort, never()).getOrderList(any());
            verify(orderCachePort, never()).putOrderList(any(), any());
            verify(orderReadRepository).findPage(any(OrderQueryCondition.class));
        }
    }

    private static ProductDetail testProductDetail() {
        return new ProductDetail("300", "测试商品", new BigDecimal("99.99"), "1", List.of("http://img.jpg"), null, null);
    }
}
