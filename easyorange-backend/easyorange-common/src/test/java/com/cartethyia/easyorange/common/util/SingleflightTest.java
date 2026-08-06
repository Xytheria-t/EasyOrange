package com.cartethyia.easyorange.common.util;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Singleflight 缓存击穿防护")
class SingleflightTest {

    @Test
    @DisplayName("同一 key 并发请求只执行一次 supplier")
    void execute_concurrentRequests_singleExecution() throws InterruptedException {
        var singleflight = new Singleflight<String, String>();
        var callCount = new AtomicInteger(0);
        var startLatch = new CountDownLatch(1);
        int threadCount = 10;
        var doneLatch = new CountDownLatch(threadCount);
        var results = new java.util.concurrent.ConcurrentLinkedQueue<String>();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                        try {
                            startLatch.await();
                            var result = singleflight.execute("same-key", () -> {
                                callCount.incrementAndGet();
                                sleepQuietly(50);
                                return "computed-value";
                            });
                            results.add(result);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            doneLatch.countDown();
                        }
                    })
                    .start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertThat(callCount.get()).as("supplier 应只执行 1 次").isEqualTo(1);
        assertThat(results).hasSize(threadCount);
        assertThat(results).containsOnly("computed-value");
    }

    @Test
    @DisplayName("supplier 抛异常时调用方收到原始异常类型")
    void execute_supplierThrows_propagatesOriginalException() {
        var singleflight = new Singleflight<String, String>();

        assertThatThrownBy(() -> singleflight.execute("err-key", () -> {
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    @Test
    @DisplayName("不同 key 独立执行")
    void execute_sequentialKeys_independentExecution() {
        var singleflight = new Singleflight<String, Integer>();
        var callCount = new AtomicInteger(0);

        var r1 = singleflight.execute("key-a", () -> {
            callCount.incrementAndGet();
            return 1;
        });
        var r2 = singleflight.execute("key-b", () -> {
            callCount.incrementAndGet();
            return 2;
        });

        assertThat(r1).isEqualTo(1);
        assertThat(r2).isEqualTo(2);
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("完成后 entry 被清理，二次调用会重新执行 supplier")
    void execute_afterCompletion_mapCleanedUp() {
        var singleflight = new Singleflight<String, Integer>();
        var callCount = new AtomicInteger(0);

        singleflight.execute("key", () -> {
            callCount.incrementAndGet();
            return 1;
        });
        singleflight.execute("key", () -> {
            callCount.incrementAndGet();
            return 2;
        });

        assertThat(callCount.get()).as("entry 清理后二次调用应重新执行 supplier").isEqualTo(2);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
