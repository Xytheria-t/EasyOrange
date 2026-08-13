package com.cartethyia.easyorange.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.payment.application.command.PaymentPhaseExecutor;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentReconstructSpec;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.port.PaymentGatewayPort;
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
@DisplayName("PaymentPhaseExecutor 测试")
class PaymentPhaseExecutorTest {

    @Mock
    private PaymentRepositoryPort paymentRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private PaymentGatewayPort paymentGateway;

    @InjectMocks
    private PaymentPhaseExecutor phaseExecutor;

    @Captor
    private ArgumentCaptor<Payment> aggregateCaptor;

    private Payment testAggregate;

    @BeforeEach
    void setUp() {
        testAggregate = buildAggregate(PaymentStatus.PENDING);
    }

    @Nested
    @DisplayName("支付两阶段")
    class PayPhaseTests {

        @Test
        @DisplayName("阶段1预处理：PENDING → PAYING")
        void preparePayPhase1_success() {
            when(paymentRepository.findByPaymentNo("PAY123")).thenReturn(Optional.of(testAggregate));

            String paymentId = phaseExecutor.preparePayPhase1("PAY123");

            assertThat(paymentId).isEqualTo("1001");
            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.PAYING);
        }

        @Test
        @DisplayName("阶段2确认：PAYING → SUCCESS 并发布事件")
        void confirmPayPhase2_success() {
            Payment payingAggregate = buildAggregate(PaymentStatus.PAYING);
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(payingAggregate));

            phaseExecutor.confirmPayPhase2("1001", PaymentResult.success("TXN_123"));

            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.SUCCESS);
            verify(domainEventPublisher).publish(any());
        }

        @Test
        @DisplayName("支付记录不存在抛出异常")
        void preparePayPhase1_notFound() {
            when(paymentRepository.findByPaymentNo("NOT_EXIST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> phaseExecutor.preparePayPhase1("NOT_EXIST"))
                    .isInstanceOf(PaymentDomainException.class);
        }

        @Test
        @DisplayName("回退支付状态：PAYING → PENDING")
        void rollbackPayStatus_success() {
            Payment payingAggregate = buildAggregate(PaymentStatus.PAYING);
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(payingAggregate));

            phaseExecutor.rollbackPayStatus("1001");

            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("退款两阶段")
    class RefundPhaseTests {

        @Test
        @DisplayName("阶段1预处理：SUCCESS → REFUNDING")
        void prepareRefundPhase1_success() {
            Payment paidAggregate = buildAggregate(PaymentStatus.SUCCESS);
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(paidAggregate));

            phaseExecutor.prepareRefundPhase1("1001", new BigDecimal("100.00"));

            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.REFUNDING);
        }

        @Test
        @DisplayName("阶段1预处理：退款金额为空时以支付金额为默认")
        void prepareRefundPhase1_nullAmount_usesAggregateAmount() {
            Payment paidAggregate = buildAggregate(PaymentStatus.SUCCESS);
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(paidAggregate));

            phaseExecutor.prepareRefundPhase1("1001", null);

            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.REFUNDING);
        }

        @Test
        @DisplayName("阶段2确认：REFUNDING → REFUNDED 并发布事件")
        void confirmRefundPhase2_success() {
            Payment refundingAggregate = buildAggregate(PaymentStatus.REFUNDING);
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(refundingAggregate));

            phaseExecutor.confirmRefundPhase2("1001", RefundResult.success("REF_123"), new BigDecimal("100.00"));

            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.REFUNDED);
            verify(domainEventPublisher).publish(any());
        }

        @Test
        @DisplayName("回退退款状态：REFUNDING → SUCCESS")
        void rollbackRefundStatus_success() {
            Payment refundingAggregate = buildAggregate(PaymentStatus.REFUNDING);
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(refundingAggregate));

            phaseExecutor.rollbackRefundStatus("1001");

            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.SUCCESS);
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
