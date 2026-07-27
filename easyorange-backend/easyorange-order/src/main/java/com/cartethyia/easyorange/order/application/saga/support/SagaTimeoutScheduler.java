package com.cartethyia.easyorange.order.application.saga.support;

import com.cartethyia.easyorange.order.domain.saga.SagaState;
import com.cartethyia.easyorange.order.domain.saga.SagaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga 超时检测调度器 — 每分钟扫描活跃 saga，标记超时与人工介入。
 * <p>
 * 检测逻辑：
 * <ol>
 *   <li>查找状态为 PENDING / COMPENSATING 且 updatedAt 早于 {@value #TIMEOUT_MINUTES} 分钟前的 saga</li>
 *   <li>重试次数已达 {@link SagaStatus#MAX_RETRY_COUNT} → 标记 {@link SagaState#MANUAL_INTERVENTION}</li>
 *   <li>否则 → 标记 {@link SagaState#TIMEOUT}</li>
 * </ol>
 * 超时 saga 不会自动重试，需运维通过管理后台或脚本介入处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaTimeoutScheduler {

    /** 超时阈值（分钟）— saga 在活跃状态超过此时间未更新视为超时 */
    static final int TIMEOUT_MINUTES = 30;

    private final SagaCoordinator sagaCoordinator;

    /**
     * 每分钟扫描超时 saga。
     * <p>
     * fixedDelay=60000 确保上次执行完成后等待 60s 再开始下一次，避免并发扫描重叠。
     */
    @Scheduled(fixedDelay = 60_000)
    public void detectTimeouts() {
        var threshold = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<SagaStatus> timedOut;

        try {
            timedOut = sagaCoordinator.findTimedOut(threshold);
        } catch (Exception e) {
            log.error("action=saga_timeout_scan_failed, error={}", e.getMessage(), e);
            return;
        }

        if (timedOut.isEmpty()) {
            return;
        }

        log.warn("action=saga_timeout_detected, count={}", timedOut.size());

        for (var saga : timedOut) {
            try {
                var newState = saga.needsManualIntervention()
                        ? SagaState.MANUAL_INTERVENTION
                        : SagaState.TIMEOUT;
                sagaCoordinator.transitionTo(saga, newState, "TIMEOUT_DETECTED");
                log.warn("action=saga_state_transition, sagaId={}, from={}, to={}, retryCount={}",
                        saga.sagaId(), saga.state(), newState, saga.retryCount());
            } catch (Exception e) {
                log.error("action=saga_timeout_update_failed, sagaId={}, error={}",
                        saga.sagaId(), e.getMessage(), e);
            }
        }
    }
}
