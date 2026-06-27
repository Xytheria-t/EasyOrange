package com.cartethyia.easyorange.order.adapter.outbound.cache;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.adapter.outbound.cache.RedisOrderCacheAdapter;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("订单缓存适配器测试")
class OrderCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisOrderCacheAdapter orderCachePort;

    private Long testBuyerId;
    private PageResult<OrderVO> testOrderPage;

    @BeforeEach
    void setUp() {
        orderCachePort = new RedisOrderCacheAdapter(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        testBuyerId = 999999L;

        OrderVO order1 = OrderVO.builder()
                .id(1L)
                .orderNo("TEST001")
                .buyerId(testBuyerId)
                .totalAmount(new BigDecimal("99.99"))
                .status(0)
                .createTime(LocalDateTime.now())
                .build();

        OrderVO order2 = OrderVO.builder()
                .id(2L)
                .orderNo("TEST002")
                .buyerId(testBuyerId)
                .totalAmount(new BigDecimal("199.99"))
                .status(1)
                .createTime(LocalDateTime.now())
                .build();

        testOrderPage = PageResult.of(List.of(order1, order2), 2, 1, 10);
    }

    @Test
    @DisplayName("设置和获取订单列表缓存")
    void testPutAndGetOrderListCache() {
        String cacheKey = "eo:order:list:999999:status:0:page:1:size:10";
        when(valueOperations.get(cacheKey)).thenReturn(testOrderPage);

        orderCachePort.putOrderList(cacheKey, testOrderPage);
        Optional<PageResult<OrderVO>> cachedResult = orderCachePort.getOrderList(cacheKey);

        assertThat(cachedResult).isPresent();
        assertThat(cachedResult.get().records()).hasSize(2);
        assertThat(cachedResult.get().total()).isEqualTo(2);

        verify(valueOperations).set(eq(cacheKey), eq(testOrderPage), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("获取不存在的订单缓存")
    void testGetNonExistentOrderCache() {
        String cacheKey = "eo:order:list:999998:status:0:page:1:size:10";
        when(valueOperations.get(cacheKey)).thenReturn(null);

        Optional<PageResult<OrderVO>> cachedResult = orderCachePort.getOrderList(cacheKey);

        assertThat(cachedResult).isEmpty();
    }

    @Test
    @DisplayName("删除订单列表缓存")
    void testEvictOrderListCache() {
        String cacheKey = "eo:order:list:999999:status:0:page:1:size:10";

        orderCachePort.evictOrderList(cacheKey);

        verify(redisTemplate).delete(cacheKey);
    }

    @Test
    @DisplayName("buildOrderListKey 构建正确的缓存键")
    void testBuildOrderListKey() {
        String keyWithStatus = orderCachePort.buildOrderListKey(123L, 1);
        assertThat(keyWithStatus).isEqualTo("eo:order:list:123:status:1:page:1:size:10");

        String keyWithoutStatus = orderCachePort.buildOrderListKey(123L, null);
        assertThat(keyWithoutStatus).isEqualTo("eo:order:list:123:status:all:page:1:size:10");
    }

    @Test
    @DisplayName("null cacheKey 不执行操作")
    void testNullCacheKey_skipsOperation() {
        orderCachePort.putOrderList(null, testOrderPage);
        orderCachePort.getOrderList(null);
        orderCachePort.evictOrderList(null);

        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
        verify(valueOperations, never()).get(anyString());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("清除认领方订单缓存")
    void testEvictBuyerOrders() {
        Long buyerId = 123456L;
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        orderCachePort.evictBuyerOrders(buyerId);

        verify(redisTemplate).keys("eo:order:list:123456:*");
    }

    @Test
    @DisplayName("清除资产方订单缓存")
    void testEvictSellerOrders() {
        Long sellerId = 789012L;
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        orderCachePort.evictSellerOrders(sellerId);

        verify(redisTemplate).keys("eo:order:list:789012:*");
    }

    @Test
    @DisplayName("清除订单缓存同时清除认领方和资产方缓存")
    void testEvictOrderCache() {
        Long buyerId = 111222L;
        Long sellerId = 333444L;
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        orderCachePort.evictOrderCache(buyerId, sellerId);

        verify(redisTemplate).keys("eo:order:list:111222:*");
        verify(redisTemplate).keys("eo:order:list:333444:*");
    }

    @Test
    @DisplayName("清除订单缓存时认领方 ID 为 null")
    void testEvictOrderCacheWithNullBuyerId() {
        Long sellerId = 555666L;
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        orderCachePort.evictOrderCache(null, sellerId);

        verify(redisTemplate, never()).keys("eo:order:list:null:*");
        verify(redisTemplate).keys("eo:order:list:555666:*");
    }

    @Test
    @DisplayName("清除订单缓存时资产方 ID 为 null")
    void testEvictOrderCacheWithNullSellerId() {
        Long buyerId = 777888L;
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        orderCachePort.evictOrderCache(buyerId, null);

        verify(redisTemplate).keys("eo:order:list:777888:*");
        verify(redisTemplate, never()).keys("eo:order:list:null:*");
    }

    @Test
    @DisplayName("null ID 不执行清除操作")
    void testEvictOrderCacheWithNullIds() {
        orderCachePort.evictOrderCache(null, null);

        verify(redisTemplate, never()).keys(anyString());
    }
}