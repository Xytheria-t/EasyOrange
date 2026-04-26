package com.cartethyia.easyorange.order.application.cache;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.dto.vo.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("订单缓存服务测试")
class OrderCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private OrderCacheService orderCacheService;

    private Long testBuyerId;
    private PageResult<OrderVO> testOrderPage;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        orderCacheService = new OrderCacheService(redisTemplate);

        testBuyerId = 999999L;

        OrderVO order1 = OrderVO.builder()
                .id(1L)
                .orderNo("TEST001")
                .buyerId(testBuyerId)
                .amount(new BigDecimal("99.99"))
                .status(0)
                .createTime(LocalDateTime.now())
                .build();

        OrderVO order2 = OrderVO.builder()
                .id(2L)
                .orderNo("TEST002")
                .buyerId(testBuyerId)
                .amount(new BigDecimal("199.99"))
                .status(1)
                .createTime(LocalDateTime.now())
                .build();

        testOrderPage = PageResult.of(List.of(order1, order2), 2, 1, 10);
    }

    @Test
    @DisplayName("设置和获取订单列表缓存")
    void testSetAndGetOrderListCache() {
        String cacheKey = orderCacheService.buildOrderListKey(testBuyerId, 0);
        when(valueOperations.get(cacheKey)).thenReturn(testOrderPage);

        orderCacheService.setOrderListCache(cacheKey, testOrderPage);
        PageResult<OrderVO> cachedResult = orderCacheService.getOrderListCache(cacheKey);

        assertThat(cachedResult).isNotNull();
        assertThat(cachedResult.records()).hasSize(2);
        assertThat(cachedResult.total()).isEqualTo(2);

        verify(valueOperations).set(eq(cacheKey), eq(testOrderPage), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("获取不存在的订单缓存")
    void testGetNonExistentOrderCache() {
        String cacheKey = orderCacheService.buildOrderListKey(999998L, 0);
        when(valueOperations.get(cacheKey)).thenReturn(null);

        PageResult<OrderVO> cachedResult = orderCacheService.getOrderListCache(cacheKey);

        assertThat(cachedResult).isNull();
    }

    @Test
    @DisplayName("删除订单列表缓存")
    void testDeleteOrderListCache() {
        String cacheKey = orderCacheService.buildOrderListKey(testBuyerId, 0);
        when(redisTemplate.delete(cacheKey)).thenReturn(true);

        orderCacheService.deleteOrderListCache(cacheKey);

        verify(redisTemplate).delete(cacheKey);
    }

    @Test
    @DisplayName("buildOrderListKey 构建正确的缓存键")
    void testBuildOrderListKey() {
        String keyWithStatus = orderCacheService.buildOrderListKey(123L, 1);
        assertThat(keyWithStatus).isEqualTo("order:list:123:1");

        String keyWithoutStatus = orderCacheService.buildOrderListKey(123L, null);
        assertThat(keyWithoutStatus).isEqualTo("order:list:123:all");
    }

    @Test
    @DisplayName("null cacheKey 不执行操作")
    void testNullCacheKey_skipsOperation() {
        orderCacheService.setOrderListCache(null, testOrderPage);
        orderCacheService.getOrderListCache(null);
        orderCacheService.deleteOrderListCache(null);

        verify(valueOperations, never()).set(any(), any(), any(Long.class), any(TimeUnit.class));
    }

    @Test
    @DisplayName("删除买家订单缓存")
    void testDeleteBuyerOrderCache() {
        Long buyerId = 123456L;

        orderCacheService.deleteBuyerOrderCache(buyerId);

        for (int status = 0; status <= 5; status++) {
            String expectedKey = "order:list:" + buyerId + ":" + status;
            verify(redisTemplate).delete(expectedKey);
        }

        String expectedAllKey = "order:list:" + buyerId + ":all";
        verify(redisTemplate).delete(expectedAllKey);
    }

    @Test
    @DisplayName("删除卖家订单缓存")
    void testDeleteSellerOrderCache() {
        Long sellerId = 789012L;

        orderCacheService.deleteSellerOrderCache(sellerId);

        for (int status = 0; status <= 5; status++) {
            String expectedKey = "order:list:" + sellerId + ":" + status;
            verify(redisTemplate).delete(expectedKey);
        }

        String expectedAllKey = "order:list:" + sellerId + ":all";
        verify(redisTemplate).delete(expectedAllKey);
    }

    @Test
    @DisplayName("删除订单缓存同时清除买家和卖家缓存")
    void testDeleteOrderCache() {
        Long buyerId = 111222L;
        Long sellerId = 333444L;

        orderCacheService.deleteOrderCache(buyerId, sellerId);

        for (int status = 0; status <= 5; status++) {
            String expectedBuyerKey = "order:list:" + buyerId + ":" + status;
            String expectedSellerKey = "order:list:" + sellerId + ":" + status;
            verify(redisTemplate).delete(expectedBuyerKey);
            verify(redisTemplate).delete(expectedSellerKey);
        }

        String expectedBuyerAllKey = "order:list:" + buyerId + ":all";
        String expectedSellerAllKey = "order:list:" + sellerId + ":all";
        verify(redisTemplate).delete(expectedBuyerAllKey);
        verify(redisTemplate).delete(expectedSellerAllKey);
    }

    @Test
    @DisplayName("删除订单缓存时买家 ID 为 null")
    void testDeleteOrderCacheWithNullBuyerId() {
        Long sellerId = 555666L;

        orderCacheService.deleteOrderCache(null, sellerId);

        for (int status = 0; status <= 5; status++) {
            String expectedSellerKey = "order:list:" + sellerId + ":" + status;
            verify(redisTemplate).delete(expectedSellerKey);
        }

        String expectedSellerAllKey = "order:list:" + sellerId + ":all";
        verify(redisTemplate).delete(expectedSellerAllKey);

        verifyNoMoreInteractions(redisTemplate);
    }

    @Test
    @DisplayName("删除订单缓存时卖家 ID 为 null")
    void testDeleteOrderCacheWithNullSellerId() {
        Long buyerId = 777888L;

        orderCacheService.deleteOrderCache(buyerId, null);

        for (int status = 0; status <= 5; status++) {
            String expectedBuyerKey = "order:list:" + buyerId + ":" + status;
            verify(redisTemplate).delete(expectedBuyerKey);
        }

        String expectedBuyerAllKey = "order:list:" + buyerId + ":all";
        verify(redisTemplate).delete(expectedBuyerAllKey);

        verifyNoMoreInteractions(redisTemplate);
    }

    @Test
    @DisplayName("null ID 不执行删除操作")
    void testDeleteOrderCacheWithNullIds() {
        orderCacheService.deleteOrderCache(null, null);

        verify(redisTemplate, never()).delete(anyString());
    }
}