package com.cartethyia.easyorange.order.application.saga.support;

import com.cartethyia.easyorange.order.domain.saga.SagaState;
import com.cartethyia.easyorange.order.domain.saga.SagaStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SagaTimeoutScheduler 测试")
class SagaTimeoutSchedulerTest {

    @Mock
    private SagaCoordinator sagaCoordinator;

    @InjectMocks
    private SagaTimeoutScheduler scheduler;

    @Test
    @DisplayName("无超时 saga 时不做任何状态转换")
    void noTimeouts_noAction() {
        when(sagaCoordinator.findTimedOut(any())).thenReturn(List.of());

        scheduler.detectTimeouts();

        verify(sagaCoordinator).findTimedOut(any());
        verifyNoMoreInteractions(sagaCoordinator);
    }

    @Test
    @DisplayName("超时且重试次数未耗尽的 saga 标记为 TIMEOUT")
    void timedOut_belowMaxRetry_markedAsTimeout() {
        var saga = createSaga(SagaState.PENDING, 1);
        when(sagaCoordinator.findTimedOut(any())).thenReturn(List.of(saga));

        scheduler.detectTimeouts();

        verify(sagaCoordinator).transitionTo(eq(saga), eq(SagaState.TIMEOUT), eq("TIMEOUT_DETECTED"));
    }

    @Test
    @DisplayName("超时且重试次数耗尽的 saga 标记为 MANUAL_INTERVENTION")
    void timedOut_maxRetryReached_markedAsManualIntervention() {
        var saga = createSaga(SagaState.COMPENSATING, SagaStatus.MAX_RETRY_COUNT);
        when(sagaCoordinator.findTimedOut(any())).thenReturn(List.of(saga));

        scheduler.detectTimeouts();

        verify(sagaCoordinator).transitionTo(eq(saga), eq(SagaState.MANUAL_INTERVENTION), eq("TIMEOUT_DETECTED"));
    }

    @Test
    @DisplayName("多个超时 saga 分别根据重试次数标记不同状态")
    void multipleTimeouts_mixedStates() {
        var saga1 = createSaga(SagaState.PENDING, 0);
        var saga2 = createSaga(SagaState.COMPENSATING, SagaStatus.MAX_RETRY_COUNT);
        when(sagaCoordinator.findTimedOut(any())).thenReturn(List.of(saga1, saga2));

        scheduler.detectTimeouts();

        var stateCaptor = ArgumentCaptor.forClass(SagaState.class);
        verify(sagaCoordinator, times(2)).transitionTo(any(SagaStatus.class), stateCaptor.capture(), eq("TIMEOUT_DETECTED"));
        assertThat(stateCaptor.getAllValues()).containsExactly(SagaState.TIMEOUT, SagaState.MANUAL_INTERVENTION);
    }

    @Test
    @DisplayName("查询异常时不抛出，仅记录日志")
    void queryException_swallowed() {
        when(sagaCoordinator.findTimedOut(any())).thenThrow(new RuntimeException("DB connection lost"));

        scheduler.detectTimeouts(); // should not throw

        verify(sagaCoordinator, never()).transitionTo(any(), any(), any());
    }

    @Test
    @DisplayName("单个 saga 更新异常不影响其他 saga 处理")
    void updateException_doesNotBlockOthers() {
        var saga1 = createSaga(SagaState.PENDING, 0);
        var saga2 = createSaga(SagaState.PENDING, 1);
        when(sagaCoordinator.findTimedOut(any())).thenReturn(List.of(saga1, saga2));
        doThrow(new RuntimeException("Lock conflict"))
                .when(sagaCoordinator).transitionTo(eq(saga1), any(), any());

        scheduler.detectTimeouts();

        verify(sagaCoordinator).transitionTo(eq(saga1), eq(SagaState.TIMEOUT), eq("TIMEOUT_DETECTED"));
        verify(sagaCoordinator).transitionTo(eq(saga2), eq(SagaState.TIMEOUT), eq("TIMEOUT_DETECTED"));
    }

    private SagaStatus createSaga(SagaState state, int retryCount) {
        return new SagaStatus(
                "saga-" + System.nanoTime(),
                "CREATE_ORDER",
                state,
                "INIT",
                "{}",
                null,
                null,
                retryCount,
                LocalDateTime.now().minusMinutes(60),
                LocalDateTime.now().minusMinutes(60)
        );
    }
}
