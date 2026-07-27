package com.cartethyia.easyorange.order.domain.saga;

import java.time.LocalDateTime;

public record SagaStatus(
    String sagaId,
    String sagaType,
    SagaState state,
    String currentStep,
    String payload,
    String errorMessage,
    String compensationLog,
    int retryCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static final int MAX_RETRY_COUNT = 3;

    public boolean canRetry() {
        return retryCount < MAX_RETRY_COUNT
                && state != SagaState.COMPLETED
                && state != SagaState.COMPENSATED
                && state != SagaState.TIMEOUT
                && state != SagaState.MANUAL_INTERVENTION;
    }

    /**
     * 判断 saga 是否处于需要超时检测的活跃状态。
     */
    public boolean isTimedOutCandidate() {
        return state == SagaState.PENDING || state == SagaState.COMPENSATING;
    }

    /**
     * 判断 saga 是否需要升级为人工介入（重试次数已耗尽）。
     */
    public boolean needsManualIntervention() {
        return retryCount >= MAX_RETRY_COUNT;
    }

    public SagaStatus withState(SagaState newState) {
        return new SagaStatus(
            sagaId, sagaType, newState, currentStep, payload,
            errorMessage, compensationLog, retryCount, createdAt, LocalDateTime.now()
        );
    }

    public SagaStatus withError(String error) {
        return new SagaStatus(
            sagaId, sagaType, SagaState.FAILED, currentStep, payload,
            error, compensationLog, retryCount, createdAt, LocalDateTime.now()
        );
    }

    public SagaStatus withRetry() {
        return new SagaStatus(
            sagaId, sagaType, state, currentStep, payload,
            errorMessage, compensationLog, retryCount + 1, createdAt, LocalDateTime.now()
        );
    }

    public SagaStatus withStep(String step) {
        return new SagaStatus(
            sagaId, sagaType, state, step, payload,
            errorMessage, compensationLog, retryCount, createdAt, LocalDateTime.now()
        );
    }

    public SagaStatus withCompensationLog(String log) {
        return new SagaStatus(
            sagaId, sagaType, state, currentStep, payload,
            errorMessage, log, retryCount, createdAt, LocalDateTime.now()
        );
    }
}
