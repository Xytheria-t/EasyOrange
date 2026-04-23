package com.cartethyia.easyorange.order.application.cache;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.dto.vo.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订单缓存服务测试
 * 
 * @author cartethyia
 * @date 2026/04/23
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("订单缓存服务测试")
class OrderCacheServiceTest {

    @Autowired
    private OrderCacheService orderCacheService;

    private Long testBuyerId;
    private PageResult<OrderVO> testOrderPage;

    @BeforeEach
    void setUp() {
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
        
        orderCacheService.setOrderListCache(cacheKey, testOrderPage);
        
        PageResult<OrderVO> cachedResult = orderCacheService.getOrderListCache(cacheKey);
        
        assertThat(cachedResult).isNotNull();
        assertThat(cachedResult.getRecords()).hasSize(2);
        assertThat(cachedResult.getTotal()).isEqualTo(2);
    }

    @Test
    @DisplayName("获取不存在的订单缓存")
    void testGetNonExistentOrderCache() {
        String cacheKey = orderCacheService.buildOrderListKey(999998L, 0);
        
        PageResult<OrderVO> cachedResult = orderCacheService.getOrderListCache(cacheKey);
        
        assertThat(cachedResult).isNull();
    }

    @Test
    @DisplayName("删除订单列表缓存")
    void testDeleteOrderListCache() {
        String cacheKey = orderCacheService.buildOrderListKey(testBuyerId, 0);
        
        orderCacheService.setOrderListCache(cacheKey, testOrderPage);
        
        orderCacheService.deleteOrderListCache(cacheKey);
        
        PageResult<OrderVO> cachedResult = orderCacheService.getOrderListCache(cacheKey);
        assertThat(cachedResult).isNull();
    }

    @Test
    @DisplayName("删除买家订单缓存")
    void testDeleteBuyerOrderCache() {
        for (int status = 0; status <= 5; status++) {
            String cacheKey = orderCacheService.buildOrderListKey(testBuyerId, status);
            orderCacheService.setOrderListCache(cacheKey, testOrderPage);
        }
        
        orderCacheService.deleteBuyerOrderCache(testBuyerId);
        
        for (int status = 0; status <= 5; status++) {
            String cacheKey = orderCacheService.buildOrderListKey(testBuyerId, status);
            PageResult<OrderVO> cachedResult = orderCacheService.getOrderListCache(cacheKey);
            assertThat(cachedResult).isNull();
        }
    }

    @Test
    @DisplayName("性能测试：大量订单缓存读写")
    void testPerformance() {
        int count = 100;
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < count; i++) {
            Long buyerId = 8000000L + i;
            String cacheKey = orderCacheService.buildOrderListKey(buyerId, null);
            
            OrderVO order = OrderVO.builder()
                    .id((long) i)
                    .orderNo("PERF" + i)
                    .buyerId(buyerId)
                    .amount(new BigDecimal("99.99"))
                    .status(0)
                    .createTime(LocalDateTime.now())
                    .build();
            
            PageResult<OrderVO> orderPage = PageResult.of(List.of(order), 1, 1, 10);
            orderCacheService.setOrderListCache(cacheKey, orderPage);
        }
        
        long setEndTime = System.currentTimeMillis();
        
        for (int i = 0; i < count; i++) {
            Long buyerId = 8000000L + i;
            String cacheKey = orderCacheService.buildOrderListKey(buyerId, null);
            
            PageResult<OrderVO> cachedResult = orderCacheService.getOrderListCache(cacheKey);
            assertThat(cachedResult).isNotNull();
        }
        
        long getEndTime = System.currentTimeMillis();
        
        System.out.println("设置 " + count + " 个订单缓存耗时：" + (setEndTime - startTime) + "ms");
        System.out.println("获取 " + count + " 个订单缓存耗时：" + (getEndTime - setEndTime) + "ms");
        System.out.println("总耗时：" + (getEndTime - startTime) + "ms");
        System.out.println("平均每个缓存操作耗时：" + ((getEndTime - startTime) / count) + "ms");
    }
}
