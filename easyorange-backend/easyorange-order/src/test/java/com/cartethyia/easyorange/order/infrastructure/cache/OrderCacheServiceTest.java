package com.cartethyia.easyorange.order.infrastructure.cache;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.adapter.inbound.web.dto.response.OrderVO;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
        Optional<PageResult<OrderVO>> cachedResult = orderCacheService.getOrderListCache(cacheKey);

        assertThat(cachedResult).isPresent();
        assertThat(cachedResult.get().records()).hasSize(2);
        assertThat(cachedResult.get().total()).isEqualTo(2);

        verify(valueOperations).set(eq(cacheKey), eq(testOrderPage), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("获取不存在的订单缓存")
    void testGetNonExistentOrderCache() {
        String cacheKey = orderCacheService.buildOrderListKey(999998L, 0);
        when(valueOperations.get(cacheKey)).thenReturn(null);

        Optional<PageResult<OrderVO>> cachedResult = orderCacheService.getOrderListCache(cacheKey);

        assertThat(cachedResult).isEmpty();
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
        assertThat(keyWithStatus).isEqualTo("eo:order:list:123:status:1:page:1:size:10");

        String keyWithoutStatus = orderCacheService.buildOrderListKey(123L, null);
        assertThat(keyWithoutStatus).isEqualTo("eo:order:list:123:status:all:page:1:size:10");
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
        String pattern = "eo:order:list:" + buyerId + ":*";
        Set<String> matchedKeys = Set.of(
            "eo:order:list:" + buyerId + ":status:0:page:1:size:10",
            "eo:order:list:" + buyerId + ":status:1:page:1:size:10"
        );
        when(redisTemplate.keys(pattern)).thenReturn(matchedKeys);

        orderCacheService.deleteBuyerOrderCache(buyerId);

        verify(redisTemplate).keys(pattern);
        verify(redisTemplate).delete(matchedKeys);
    }

    @Test
    @DisplayName("删除卖家订单缓存")
    void testDeleteSellerOrderCache() {
        Long sellerId = 789012L;
        String pattern = "eo:order:list:" + sellerId + ":*";
        Set<String> matchedKeys = Set.of(
            "eo:order:list:" + sellerId + ":status:0:page:1:size:10"
        );
        when(redisTemplate.keys(pattern)).thenReturn(matchedKeys);

        orderCacheService.deleteSellerOrderCache(sellerId);

        verify(redisTemplate).keys(pattern);
        verify(redisTemplate).delete(matchedKeys);
    }

    @Test
    @DisplayName("删除订单缓存同时清除买家和卖家缓存")
    void testDeleteOrderCache() {
        Long buyerId = 111222L;
        Long sellerId = 333444L;
        String buyerPattern = "eo:order:list:" + buyerId + ":*";
        String sellerPattern = "eo:order:list:" + sellerId + ":*";
        Set<String> buyerKeys = Set.of("eo:order:list:" + buyerId + ":status:0:page:1:size:10");
        Set<String> sellerKeys = Set.of("eo:order:list:" + sellerId + ":status:0:page:1:size:10");
        when(redisTemplate.keys(buyerPattern)).thenReturn(buyerKeys);
        when(redisTemplate.keys(sellerPattern)).thenReturn(sellerKeys);

        orderCacheService.deleteOrderCache(buyerId, sellerId);

        verify(redisTemplate).keys(buyerPattern);
        verify(redisTemplate).delete(buyerKeys);
        verify(redisTemplate).keys(sellerPattern);
        verify(redisTemplate).delete(sellerKeys);
    }

    @Test
    @DisplayName("删除订单缓存时买家 ID 为 null")
    void testDeleteOrderCacheWithNullBuyerId() {
        Long sellerId = 555666L;
        String sellerPattern = "eo:order:list:" + sellerId + ":*";
        Set<String> sellerKeys = Set.of("eo:order:list:" + sellerId + ":status:0:page:1:size:10");
        when(redisTemplate.keys(sellerPattern)).thenReturn(sellerKeys);

        orderCacheService.deleteOrderCache(null, sellerId);

        verify(redisTemplate).keys(sellerPattern);
        verify(redisTemplate).delete(sellerKeys);
        verify(redisTemplate, never()).keys(contains("null"));
    }

    @Test
    @DisplayName("删除订单缓存时卖家 ID 为 null")
    void testDeleteOrderCacheWithNullSellerId() {
        Long buyerId = 777888L;
        String buyerPattern = "eo:order:list:" + buyerId + ":*";
        Set<String> buyerKeys = Set.of("eo:order:list:" + buyerId + ":status:0:page:1:size:10");
        when(redisTemplate.keys(buyerPattern)).thenReturn(buyerKeys);

        orderCacheService.deleteOrderCache(buyerId, null);

        verify(redisTemplate).keys(buyerPattern);
        verify(redisTemplate).delete(buyerKeys);
        verify(redisTemplate, never()).keys(contains("null"));
    }

    @Test
    @DisplayName("null ID 不执行删除操作")
    void testDeleteOrderCacheWithNullIds() {
        orderCacheService.deleteOrderCache(null, null);

        verify(redisTemplate, never()).keys(anyString());
        verify(redisTemplate, never()).delete(anyString());
    }
}
