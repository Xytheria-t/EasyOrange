package com.cartethyia.easyorange.order.infrastructure.cache;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.application.dto.OrderVO;
import com.cartethyia.easyorange.order.domain.port.output.OrderCachePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.cartethyia.easyorange.order.OrderIntegrationTestConfig.class)
@Testcontainers
@Tag("integration")
@DisplayName("OrderCachePort 性能测试")
class OrderCacheServicePerformanceTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(
        DockerImageName.parse("redis:7-alpine")
    )
    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired
    private OrderCachePort orderCachePort;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASUREMENT_ITERATIONS = 1000;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Nested
    @DisplayName("缓存命中率测试")
    class CacheHitRateTests {

        @Test
        @DisplayName("订单列表缓存命中率")
        void orderListCache_hitRate(TestInfo testInfo) {
            Long userId = 1L;
            Integer status = 0;
            String cacheKey = orderCachePort.buildOrderListKey(userId, status, 1, 10);

            PageResult<OrderVO> mockResult = createMockOrderList(10);
            orderCachePort.putOrderList(cacheKey, mockResult);

            int hits = 0;
            int misses = 0;

            for (int i = 0; i < 100; i++) {
                var result = orderCachePort.getOrderList(cacheKey);
                if (result.isPresent()) {
                    hits++;
                } else {
                    misses++;
                }
            }

            double hitRate = (double) hits / (hits + misses) * 100;

            System.out.printf("[%s] 缓存命中率: %.2f%% (命中: %d, 未命中: %d)%n",
                testInfo.getDisplayName(), hitRate, hits, misses);

            assertThat(hitRate).isEqualTo(100.0);
        }

        @Test
        @DisplayName("订单详情缓存命中率")
        void orderDetailCache_hitRate(TestInfo testInfo) {
            Long orderId = 1L;
            OrderVO mockOrder = createMockOrder(orderId);

            orderCachePort.putOrderDetail(orderId, mockOrder);

            int hits = 0;
            int misses = 0;

            for (int i = 0; i < 100; i++) {
                var result = orderCachePort.getOrderDetail(orderId);
                if (result.isPresent()) {
                    hits++;
                } else {
                    misses++;
                }
            }

            double hitRate = (double) hits / (hits + misses) * 100;

            System.out.printf("[%s] 缓存命中率: %.2f%% (命中: %d, 未命中: %d)%n",
                testInfo.getDisplayName(), hitRate, hits, misses);

            assertThat(hitRate).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("性能基准测试")
    class PerformanceBenchmarkTests {

        @Test
        @DisplayName("缓存读取性能")
        void cacheRead_performance(TestInfo testInfo) {
            Long userId = 1L;
            String cacheKey = orderCachePort.buildOrderListKey(userId, 0, 1, 10);
            PageResult<OrderVO> mockResult = createMockOrderList(10);
            orderCachePort.putOrderList(cacheKey, mockResult);

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                orderCachePort.getOrderList(cacheKey);
            }

            long startTime = System.nanoTime();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                orderCachePort.getOrderList(cacheKey);
            }
            long endTime = System.nanoTime();

            double avgTimeMs = (endTime - startTime) / (double) MEASUREMENT_ITERATIONS / 1_000_000;
            double throughput = MEASUREMENT_ITERATIONS / ((endTime - startTime) / 1_000_000_000.0);

            System.out.printf("[%s] 平均读取时间: %.4f ms, 吞吐量: %.0f ops/s%n",
                testInfo.getDisplayName(), avgTimeMs, throughput);

            assertThat(avgTimeMs).isLessThan(5.0);
        }

        @Test
        @DisplayName("缓存写入性能")
        void cacheWrite_performance(TestInfo testInfo) {
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                String cacheKey = orderCachePort.buildOrderListKey((long) i, 0, 1, 10);
                orderCachePort.putOrderList(cacheKey, createMockOrderList(10));
            }

            long startTime = System.nanoTime();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                String cacheKey = orderCachePort.buildOrderListKey((long) (i + WARMUP_ITERATIONS), 0, 1, 10);
                orderCachePort.putOrderList(cacheKey, createMockOrderList(10));
            }
            long endTime = System.nanoTime();

            double avgTimeMs = (endTime - startTime) / (double) MEASUREMENT_ITERATIONS / 1_000_000;
            double throughput = MEASUREMENT_ITERATIONS / ((endTime - startTime) / 1_000_000_000.0);

            System.out.printf("[%s] 平均写入时间: %.4f ms, 吞吐量: %.0f ops/s%n",
                testInfo.getDisplayName(), avgTimeMs, throughput);

            assertThat(avgTimeMs).isLessThan(10.0);
        }

        @Test
        @DisplayName("缓存删除性能")
        void cacheDelete_performance(TestInfo testInfo) {
            List<String> keys = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                String cacheKey = orderCachePort.buildOrderListKey((long) i, 0, 1, 10);
                orderCachePort.putOrderList(cacheKey, createMockOrderList(10));
                keys.add(cacheKey);
            }

            long startTime = System.nanoTime();
            for (String key : keys) {
                orderCachePort.evictOrderList(key);
            }
            long endTime = System.nanoTime();

            double avgTimeMs = (endTime - startTime) / (double) MEASUREMENT_ITERATIONS / 1_000_000;
            double throughput = MEASUREMENT_ITERATIONS / ((endTime - startTime) / 1_000_000_000.0);

            System.out.printf("[%s] 平均删除时间: %.4f ms, 吞吐量: %.0f ops/s%n",
                testInfo.getDisplayName(), avgTimeMs, throughput);

            assertThat(avgTimeMs).isLessThan(5.0);
        }
    }

    @Nested
    @DisplayName("缓存策略效果测试")
    class CacheStrategyEffectivenessTests {

        @Test
        @DisplayName("细粒度缓存键策略效果")
        void fineGrainedCacheKey_effectiveness(TestInfo testInfo) {
            Long userId = 1L;

            for (int status = 0; status <= 5; status++) {
                for (int page = 1; page <= 3; page++) {
                    String cacheKey = orderCachePort.buildOrderListKey(userId, status, page, 10);
                    orderCachePort.putOrderList(cacheKey, createMockOrderList(10));
                }
            }

            String allStatusKey = orderCachePort.buildOrderListKey(userId, null, 1, 10);
            orderCachePort.putOrderList(allStatusKey, createMockOrderList(10));

            int totalKeys = 0;
            for (int status = 0; status <= 5; status++) {
                for (int page = 1; page <= 3; page++) {
                    String cacheKey = orderCachePort.buildOrderListKey(userId, status, page, 10);
                    if (orderCachePort.getOrderList(cacheKey).isPresent()) {
                        totalKeys++;
                    }
                }
            }

            System.out.printf("[%s] 细粒度缓存键数量: %d (预期: 18)%n",
                testInfo.getDisplayName(), totalKeys);

            assertThat(totalKeys).isEqualTo(18);
        }

        @Test
        @DisplayName("缓存失效策略效果")
        void cacheInvalidation_effectiveness(TestInfo testInfo) {
            Long buyerId = 1L;
            Long sellerId = 2L;

            for (int status = 0; status <= 5; status++) {
                String buyerKey = orderCachePort.buildOrderListKey(buyerId, status, 1, 10);
                orderCachePort.putOrderList(buyerKey, createMockOrderList(10));

                String sellerKey = orderCachePort.buildOrderListKey(sellerId, status, 1, 10);
                orderCachePort.putOrderList(sellerKey, createMockOrderList(10));
            }

            orderCachePort.evictOrderCache(buyerId, sellerId);

            int remainingKeys = 0;
            for (int status = 0; status <= 5; status++) {
                String buyerKey = orderCachePort.buildOrderListKey(buyerId, status, 1, 10);
                if (orderCachePort.getOrderList(buyerKey).isPresent()) {
                    remainingKeys++;
                }

                String sellerKey = orderCachePort.buildOrderListKey(sellerId, status, 1, 10);
                if (orderCachePort.getOrderList(sellerKey).isPresent()) {
                    remainingKeys++;
                }
            }

            System.out.printf("[%s] 缓存失效后剩余键数量: %d (预期: 0)%n",
                testInfo.getDisplayName(), remainingKeys);

            assertThat(remainingKeys).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("并发性能测试")
    class ConcurrentPerformanceTests {

        @Test
        @DisplayName("并发读取性能")
        void concurrentRead_performance(TestInfo testInfo) throws InterruptedException {
            Long userId = 1L;
            String cacheKey = orderCachePort.buildOrderListKey(userId, 0, 1, 10);
            orderCachePort.putOrderList(cacheKey, createMockOrderList(10));

            int threadCount = 10;
            int iterationsPerThread = 100;
            List<Thread> threads = new ArrayList<>();

            long startTime = System.nanoTime();

            for (int i = 0; i < threadCount; i++) {
                Thread thread = new Thread(() -> {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        orderCachePort.getOrderList(cacheKey);
                    }
                });
                threads.add(thread);
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            long endTime = System.nanoTime();
            int totalOperations = threadCount * iterationsPerThread;
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;
            double throughput = totalOperations / (totalTimeMs / 1000.0);

            System.out.printf("[%s] 并发读取吞吐量: %.0f ops/s (线程数: %d, 总操作: %d, 总时间: %.2f ms)%n",
                testInfo.getDisplayName(), throughput, threadCount, totalOperations, totalTimeMs);

            assertThat(throughput).isGreaterThan(1000.0);
        }

        @Test
        @DisplayName("并发写入性能")
        void concurrentWrite_performance(TestInfo testInfo) throws InterruptedException {
            int threadCount = 10;
            int iterationsPerThread = 50;
            List<Thread> threads = new ArrayList<>();

            long startTime = System.nanoTime();

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                Thread thread = new Thread(() -> {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        Long userId = (long) (threadId * iterationsPerThread + j);
                        String cacheKey = orderCachePort.buildOrderListKey(userId, 0, 1, 10);
                        orderCachePort.putOrderList(cacheKey, createMockOrderList(10));
                    }
                });
                threads.add(thread);
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            long endTime = System.nanoTime();
            int totalOperations = threadCount * iterationsPerThread;
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;
            double throughput = totalOperations / (totalTimeMs / 1000.0);

            System.out.printf("[%s] 并发写入吞吐量: %.0f ops/s (线程数: %d, 总操作: %d, 总时间: %.2f ms)%n",
                testInfo.getDisplayName(), throughput, threadCount, totalOperations, totalTimeMs);

            assertThat(throughput).isGreaterThan(500.0);
        }
    }

    private PageResult<OrderVO> createMockOrderList(int count) {
        List<OrderVO> orders = IntStream.range(0, count)
            .mapToObj(i -> createMockOrder((long) i))
            .toList();
        return PageResult.of(orders, (long) count, 1, count);
    }

    private OrderVO createMockOrder(Long id) {
        return OrderVO.builder()
                .id(id)
                .orderNo("ORD" + id)
                .buyerId(1L)
                .sellerId(2L)
                .totalAmount(new BigDecimal("99.99"))
                .status(0)
                .address("测试地址")
                .phone("13800138000")
                .remark("测试订单")
                .build();
    }
}
