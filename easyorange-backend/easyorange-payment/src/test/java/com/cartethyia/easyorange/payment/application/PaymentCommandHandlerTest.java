package com.cartethyia.easyorange.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.lock.DistributedLockPort;
import com.cartethyia.easyorange.framework.lock.LockAcquisitionException;
import com.cartethyia.easyorange.payment.application.command.ClosePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PayCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.application.command.PaymentPhaseExecutor;
import com.cartethyia.easyorange.payment.application.command.RefundPaymentCommand;
import com.cartethyia.easyorange.payment.application.metrics.PaymentMetricsService;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentReconstructSpec;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.payment.domain.port.RefundResult;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepositoryPort;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCommandHandler 测试")
class PaymentCommandHandlerTest {

    @Mock
    private PaymentRepositoryPort paymentRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private DistributedLockPort lockPort;

    @Mock
    private PaymentMetricsService metricsService;

    @Mock
    private PaymentPhaseExecutor phaseExecutor;

    @InjectMocks
    private PaymentCommandHandler commandHandler;

    @Captor
    private ArgumentCaptor<Payment> aggregateCaptor;

    private Payment testAggregate;

    @BeforeEach
    void setUp() {
        testAggregate = buildAggregate(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("锁获取失败时记录并发冲突指标并抛支付域繁忙异常（A0429→429）")
    void executeWithLock_lockConflict_recordsMetricAndThrowsBusy() {
        doThrow(new LockAcquisitionException("busy"))
                .when(lockPort)
                .executeWithLock(anyString(), anyLong(), any(Runnable.class));

        assertThatThrownBy(() -> commandHandler.handle(new PayCommand("PAY123", null, null)))
                .isInstanceOf(PaymentDomainException.class)
                .satisfies(e -> assertThat(((PaymentDomainException) e).getCode())
                        .isEqualTo(PaymentResultCode.PAYMENT_BUSY.getCode()));

        verify(metricsService).recordConcurrentConflict();
    }

    @Nested
    @DisplayName("handle(CreatePaymentCommand)")
    class CreatePaymentTests {

        @Test
        @DisplayName("创建支付成功")
        void handle_createPayment_success() {
            CreatePaymentCommand command =
                    new CreatePaymentCommand("2001", new BigDecimal("100.00"), "WECHAT", null, "test");

            when(idGenerator.generateId()).thenReturn("1001");

            String paymentId = commandHandler.handle("3001", command);

            assertThat(paymentId).isNotNull();
            verify(paymentRepository).save(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().orderId()).isEqualTo("2001");
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.PENDING);
            verify(domainEventPublisher).publish(any());
        }
    }

    @Nested
    @DisplayName("handle(PayCommand) - 两阶段编排")
    class PayFlowTests {

        @BeforeEach
        void enableLockWrapper() {
            doAnswer(invocation -> {
                        invocation.getArgument(2, Runnable.class).run();
                        return null;
                    })
                    .when(lockPort)
                    .executeWithLock(anyString(), anyLong(), any(Runnable.class));
        }

        @Test
        @DisplayName("支付成功 - 网关成功走完整两阶段")
        void handle_pay_success() {
            when(phaseExecutor.preparePayPhase1("PAY123")).thenReturn("1001");
            when(phaseExecutor.invokePayGateway("1001")).thenReturn(PaymentResult.success("TXN_123"));

            commandHandler.handle(new PayCommand("PAY123", null, null));

            verify(phaseExecutor).confirmPayPhase2(eq("1001"), any(PaymentResult.class));
            verify(phaseExecutor, never()).rollbackPayStatus(anyString());
        }

        @Test
        @DisplayName("支付失败 - 网关失败回退 PENDING")
        void handle_pay_gatewayFailure_rollsBack() {
            when(phaseExecutor.preparePayPhase1("PAY123")).thenReturn("1001");
            when(phaseExecutor.invokePayGateway("1001")).thenReturn(PaymentResult.failure("网关拒绝"));

            commandHandler.handle(new PayCommand("PAY123", null, null));

            verify(phaseExecutor).rollbackPayStatus("1001");
            verify(phaseExecutor, never()).confirmPayPhase2(anyString(), any());
        }
    }

    @Nested
    @DisplayName("handle(RefundPaymentCommand) - 两阶段编排")
    class RefundFlowTests {

        @BeforeEach
        void enableLockWrapper() {
            doAnswer(invocation -> {
                        invocation.getArgument(2, Runnable.class).run();
                        return null;
                    })
                    .when(lockPort)
                    .executeWithLock(anyString(), anyLong(), any(Runnable.class));
        }

        @Test
        @DisplayName("退款成功 - 网关成功走完整两阶段")
        void handle_refund_success() {
            when(phaseExecutor.invokeRefundGateway("1001", new BigDecimal("100.00")))
                    .thenReturn(RefundResult.success("REF_123"));

            commandHandler.handle(new RefundPaymentCommand("1001", new BigDecimal("100.00"), "用户申请"));

            verify(phaseExecutor).prepareRefundPhase1("1001", new BigDecimal("100.00"));
            verify(phaseExecutor)
                    .confirmRefundPhase2(eq("1001"), any(RefundResult.class), eq(new BigDecimal("100.00")));
            verify(phaseExecutor, never()).rollbackRefundStatus(anyString());
        }

        @Test
        @DisplayName("退款网关失败 - 回退 SUCCESS")
        void handle_refund_gatewayFailure_rollsBack() {
            when(phaseExecutor.invokeRefundGateway("1001", new BigDecimal("100.00")))
                    .thenReturn(RefundResult.failure("网关拒绝"));

            commandHandler.handle(new RefundPaymentCommand("1001", new BigDecimal("100.00"), "用户申请"));

            verify(phaseExecutor).prepareRefundPhase1("1001", new BigDecimal("100.00"));
            verify(phaseExecutor).rollbackRefundStatus("1001");
            verify(phaseExecutor, never()).confirmRefundPhase2(anyString(), any(), any());
        }
    }

    @Nested
    @DisplayName("handle(ClosePaymentCommand)")
    class CloseCommandTests {

        @Test
        @DisplayName("关闭支付成功并发布事件")
        void handle_close_success() {
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(testAggregate));

            ClosePaymentCommand command = new ClosePaymentCommand("1001");

            commandHandler.handle(command);

            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.CLOSED);
            verify(domainEventPublisher).publish(any());
        }

        @Test
        @DisplayName("支付记录不存在抛出异常")
        void handle_close_notFound() {
            when(paymentRepository.findById("9999")).thenReturn(Optional.empty());

            ClosePaymentCommand command = new ClosePaymentCommand("9999");

            assertThatThrownBy(() -> commandHandler.handle(command)).isInstanceOf(PaymentDomainException.class);
        }
    }

    // ==================== Fixture ====================

    /**
     * 构造测试用聚合根 — 除 status 外其余字段固定，便于不同状态场景复用。
     */
    private static Payment buildAggregate(PaymentStatus status) {
        var spec = new PaymentReconstructSpec(
                "1001",
                "PAY123",
                "2001",
                "3001",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                PaymentMethod.WECHAT,
                status,
                null,
                null,
                null,
                null,
                null,
                null,
                0);
        return Payment.from(spec);
    }
}
