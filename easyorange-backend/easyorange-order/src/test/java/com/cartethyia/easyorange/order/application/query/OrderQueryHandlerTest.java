package com.cartethyia.easyorange.order.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.order.application.cache.OrderCacheService;
import com.cartethyia.easyorange.order.domain.repository.OrderReadRepository;
import com.cartethyia.easyorange.order.dto.request.QueryOrderRequest;
import com.cartethyia.easyorange.order.dto.vo.OrderVO;
import com.cartethyia.easyorange.order.entity.Order;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import com.cartethyia.easyorange.product.application.handler.ProductQueryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("订单查询处理器测试")
class OrderQueryHandlerTest {

    @Mock
    private OrderReadRepository orderReadRepository;

    @Mock
    private ProductQueryHandler productQueryHandler;

    @Mock
    private OrderCacheService orderCacheService;

    private OrderQueryHandler orderQueryHandler;

    private Long testUserId;
    private Order testOrder;
    private ProductVO testProduct;

    @BeforeEach
    void setUp() {
        orderQueryHandler = new OrderQueryHandler(orderReadRepository, productQueryHandler, orderCacheService);

        testUserId = 999999L;

        testOrder = Order.builder()
                .id(1L)
                .orderNo("ORD001")
                .buyerId(testUserId)
                .sellerId(888888L)
                .productId(777777L)
                .amount(new BigDecimal("99.99"))
                .status(0)
                .paymentStatus(0)
                .address("Test Address")
                .phone("1234567890")
                .remark("Test Remark")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        testProduct = ProductVO.builder()
                .id(777777L)
                .title("Test Product")
                .price(new BigDecimal("99.99"))
                .status(1)
                .build();
    }

    @Test
    @DisplayName("获取我的订单 - 缓存命中")
    void testGetMyOrders_CacheHit() {
        QueryOrderRequest request = QueryOrderRequest.builder()
                .status(0)
                .pageNum(1)
                .pageSize(10)
                .build();

        String cacheKey = "order:list:" + testUserId + ":0";
        when(orderCacheService.buildOrderListKey(eq(testUserId), eq(0))).thenReturn(cacheKey);

        OrderVO cachedOrderVO = OrderVO.builder()
                .id(testOrder.getId())
                .orderNo(testOrder.getOrderNo())
                .buyerId(testOrder.getBuyerId())
                .amount(testOrder.getAmount())
                .status(testOrder.getStatus())
                .build();

        PageResult<OrderVO> cachedPage = PageResult.of(List.of(cachedOrderVO), 1, 1, 10);
        when(orderCacheService.getOrderListCache(cacheKey)).thenReturn(cachedPage);

        try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
            mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(testUserId);

            PageResult<OrderVO> result = orderQueryHandler.getMyOrders(request);

            assertThat(result).isNotNull();
            assertThat(result.records()).hasSize(1);
            assertThat(result.records().getFirst().getId()).isEqualTo(testOrder.getId());

            verify(orderReadRepository, never()).findPage(any());
            verify(orderCacheService).getOrderListCache(cacheKey);
            verify(orderCacheService, never()).setOrderListCache(any(), any());
        }
    }

    @Test
    @DisplayName("获取我的订单 - 缓存未命中")
    void testGetMyOrders_CacheMiss() {
        QueryOrderRequest request = QueryOrderRequest.builder()
                .status(0)
                .pageNum(1)
                .pageSize(10)
                .build();

        String cacheKey = "order:list:" + testUserId + ":0";
        when(orderCacheService.buildOrderListKey(eq(testUserId), eq(0))).thenReturn(cacheKey);
        when(orderCacheService.getOrderListCache(cacheKey)).thenReturn(null);

        PageResult<Order> orderPage = PageResult.of(List.of(testOrder), 1, 1, 10);
        when(orderReadRepository.findPage(any(QueryOrderRequest.class))).thenReturn(orderPage);
        when(productQueryHandler.getProductsByIds(List.of(777777L))).thenReturn(List.of(testProduct));

        try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
            mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(testUserId);

            PageResult<OrderVO> result = orderQueryHandler.getMyOrders(request);

            assertThat(result).isNotNull();
            assertThat(result.records()).hasSize(1);
            assertThat(result.records().getFirst().getId()).isEqualTo(testOrder.getId());

            verify(orderReadRepository).findPage(any(QueryOrderRequest.class));
            verify(orderCacheService).setOrderListCache(eq(cacheKey), any(PageResult.class));
        }
    }

    @Test
    @DisplayName("获取卖家订单 - 缓存命中")
    void testGetSoldOrders_CacheHit() {
        QueryOrderRequest request = QueryOrderRequest.builder()
                .status(1)
                .pageNum(1)
                .pageSize(10)
                .build();

        String cacheKey = "order:list:" + testUserId + ":1";
        when(orderCacheService.buildOrderListKey(eq(testUserId), eq(1))).thenReturn(cacheKey);

        OrderVO cachedOrderVO = OrderVO.builder()
                .id(testOrder.getId())
                .orderNo(testOrder.getOrderNo())
                .sellerId(testOrder.getSellerId())
                .amount(testOrder.getAmount())
                .status(testOrder.getStatus())
                .build();

        PageResult<OrderVO> cachedPage = PageResult.of(List.of(cachedOrderVO), 1, 1, 10);
        when(orderCacheService.getOrderListCache(cacheKey)).thenReturn(cachedPage);

        try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
            mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(testUserId);

            PageResult<OrderVO> result = orderQueryHandler.getSoldOrders(request);

            assertThat(result).isNotNull();
            assertThat(result.records()).hasSize(1);

            verify(orderReadRepository, never()).findPage(any());
            verify(orderCacheService).getOrderListCache(cacheKey);
            verify(orderCacheService, never()).setOrderListCache(any(), any());
        }
    }

    @Test
    @DisplayName("获取卖家订单 - 缓存未命中")
    void testGetSoldOrders_CacheMiss() {
        QueryOrderRequest request = QueryOrderRequest.builder()
                .status(1)
                .pageNum(1)
                .pageSize(10)
                .build();

        String cacheKey = "order:list:" + testUserId + ":1";
        when(orderCacheService.buildOrderListKey(eq(testUserId), eq(1))).thenReturn(cacheKey);
        when(orderCacheService.getOrderListCache(cacheKey)).thenReturn(null);

        PageResult<Order> orderPage = PageResult.of(List.of(testOrder), 1, 1, 10);
        when(orderReadRepository.findPage(any(QueryOrderRequest.class))).thenReturn(orderPage);
        when(productQueryHandler.getProductsByIds(List.of(777777L))).thenReturn(List.of(testProduct));

        try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
            mockedStatic.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(testUserId);

            PageResult<OrderVO> result = orderQueryHandler.getSoldOrders(request);

            assertThat(result).isNotNull();
            assertThat(result.records()).hasSize(1);

            verify(orderReadRepository).findPage(any(QueryOrderRequest.class));
            verify(orderCacheService).setOrderListCache(eq(cacheKey), any(PageResult.class));
        }
    }
}
