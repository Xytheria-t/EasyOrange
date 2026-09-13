package com.cartethyia.easyorange.product.adapter.outbound.scheduler;

import com.cartethyia.easyorange.framework.metrics.BusinessMetricsService;
import com.cartethyia.easyorange.product.domain.repository.StockLedgerRepository;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 库存对账任务 — 比对资产库存余额与最近一条流水的余额快照，发现漂移即告警。
 * <p>
 * 流水是库存变更的单一事实来源，正常运行时 {@code eo_product.stock} 必然等于最新流水的
 * {@code stock_after}。不相等只有两种可能：有库存变更绕过了流水落账（新代码路径漏埋点、人工改库），
 * 或流水与余额没有同事务提交。这两种都是缺陷信号，因此只告警不自动改写余额——自动修复会让缺陷
 * 从「可观测」退回「静默」，而流水本身保留了还原真相所需的全部信息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockReconcileScheduler {

    private static final String RECONCILE_LOCK = "eo:product:stock:reconcile:lock";

    /** 单次最多上报的漂移条数——异常扩散时保留证据，但不让日志与告警被刷爆。 */
    private static final int DRIFT_LIMIT = 50;

    private final StockLedgerRepository stockLedgerRepository;
    private final BusinessMetricsService businessMetricsService;
    private final RedisTemplate<Object, Object> redisTemplate;

    @Scheduled(cron = "${easyorange.product.stock-reconcile.cron:0 20 3 * * ?}")
    public void reconcile() {
        var locked = redisTemplate.opsForValue().setIfAbsent(RECONCILE_LOCK, "1", 10, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }
        try {
            var drifts = stockLedgerRepository.findDrifts(DRIFT_LIMIT);
            if (drifts.isEmpty()) {
                log.debug("库存对账通过：流水快照与库存余额一致");
                return;
            }
            businessMetricsService.recordStockDrift(drifts.size());
            log.error("库存对账发现漂移 {} 条（疑似存在绕过流水落账的库存变更），明细: {}", drifts.size(), drifts);
        } catch (Exception e) {
            log.error("库存对账任务执行失败", e);
        } finally {
            redisTemplate.delete(RECONCILE_LOCK);
        }
    }
}
