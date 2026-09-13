package com.cartethyia.easyorange.product.adapter.outbound.scheduler;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.framework.metrics.BusinessMetricsService;
import com.cartethyia.easyorange.product.domain.repository.StockLedgerRepository;
import com.cartethyia.easyorange.product.domain.valueobject.StockDrift;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockReconcileScheduler 单元测试")
class StockReconcileSchedulerTest {

    private static final String LOCK_KEY = "eo:product:stock:reconcile:lock";

    @Mock
    private StockLedgerRepository stockLedgerRepository;

    @Mock
    private BusinessMetricsService businessMetricsService;

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private ValueOperations<Object, Object> valueOperations;

    @InjectMocks
    private StockReconcileScheduler scheduler;

    @Nested
    @DisplayName("reconcile")
    class ReconcileTests {

        @Test
        @DisplayName("无漂移时不打指标，且释放锁")
        void reconcile_noDrift_recordsNothing() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), eq(10L), eq(TimeUnit.MINUTES)))
                    .thenReturn(true);
            when(stockLedgerRepository.findDrifts(anyInt())).thenReturn(List.of());

            scheduler.reconcile();

            verify(businessMetricsService, never()).recordStockDrift(anyInt());
            verify(redisTemplate).delete(LOCK_KEY);
        }

        @Test
        @DisplayName("发现漂移时按条数打指标")
        void reconcile_withDrift_recordsMetric() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), eq(10L), eq(TimeUnit.MINUTES)))
                    .thenReturn(true);
            when(stockLedgerRepository.findDrifts(anyInt()))
                    .thenReturn(List.of(new StockDrift("p-1", 3, 2), new StockDrift("p-2", 0, 1)));

            scheduler.reconcile();

            verify(businessMetricsService).recordStockDrift(2);
        }

        @Test
        @DisplayName("未抢到锁（多实例）时不查询也不打指标")
        void reconcile_lockNotAcquired_skips() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(eq(LOCK_KEY), eq("1"), eq(10L), eq(TimeUnit.MINUTES)))
                    .thenReturn(false);

            scheduler.reconcile();

            verify(stockLedgerRepository, never()).findDrifts(anyInt());
            verify(businessMetricsService, never()).recordStockDrift(anyInt());
        }
    }
}
