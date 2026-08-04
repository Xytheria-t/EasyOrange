package com.cartethyia.easyorange.payment.application;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.payment.application.command.ClosePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PayCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.application.command.RefundPaymentCommand;
import com.cartethyia.easyorange.payment.application.lock.DistributedLockWrapper;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentReconstructSpec;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.port.PaymentGatewayPort;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepositoryPort;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.payment.domain.port.RefundResult;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCommandHandler 测试")
class PaymentCommandHandlerTest {

    @Mock
    private PaymentRepositoryPort paymentRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private PaymentGatewayPort paymentGateway;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private DistributedLockWrapper lockWrapper;

    @InjectMocks
    private PaymentCommandHandler commandHandler;

    @Captor
    private ArgumentCaptor<Payment> aggregateCaptor;

    private Payment testAggregate;

    @BeforeEach
    void setUp() {
        TestSecurityUtil.setSecurityContext("3001");
        testAggregate = buildAggregate(PaymentStatus.PENDING);
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtil.clearSecurityContext();
    }

    @Nested
    @DisplayName("handle(CreatePaymentCommand)")
    class CreatePaymentTests {

        @Test
        @DisplayName("创建支付成功")
        void handle_createPayment_success() {
            CreatePaymentCommand command = new CreatePaymentCommand(
                    "2001",
                    new BigDecimal("100.00"),
                    "WECHAT",
                    null,
                    "test"
            );

            when(idGenerator.generateId()).thenReturn("1001");

            String paymentId = commandHandler.handle(command);

            assertThat(paymentId).isNotNull();
            verify(paymentRepository).save(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().orderId()).isEqualTo("2001");
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.PENDING);
            verify(domainEventPublisher).publish(any());
        }
    }

    @Nested
    @DisplayName("handle(PayCommand) - 两阶段")
    class PayCommandTests {

        @Test
        @DisplayName("支付成功 - 阶段1预处理")
        void preparePayPhase1_success() {
            when(paymentRepository.findByPaymentNo("PAY123")).thenReturn(Optional.of(testAggregate));

            String paymentId = commandHandler.preparePayPhase1("PAY123");

            assertThat(paymentId).isEqualTo("1001");
            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.PAYING);
        }

        @Test
        @DisplayName("支付成功 - 阶段2确认")
        void confirmPayPhase2_success() {
            Payment payingAggregate = buildAggregate(PaymentStatus.PAYING);
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(payingAggregate));

            commandHandler.confirmPayPhase2("1001", PaymentResult.success("TXN_123"));

            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.SUCCESS);
            verify(domainEventPublisher).publish(any());
        }

        @Test
        @DisplayName("支付记录不存在抛出异常")
        void handle_pay_notFound() {
            when(paymentRepository.findByPaymentNo("NOT_EXIST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commandHandler.preparePayPhase1("NOT_EXIST"))
                    .isInstanceOf(PaymentDomainException.class);
        }
    }

    @Nested
    @DisplayName("handle(RefundPaymentCommand) - 两阶段")
    class RefundCommandTests {

        @Test
        @DisplayName("退款预处理成功")
        void prepareRefundPhase1_success() {
            Payment paidAggregate = buildAggregate(PaymentStatus.SUCCESS);
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(paidAggregate));

            String paymentId = commandHandler.prepareRefundPhase1("1001", new BigDecimal("100.00"));

            assertThat(paymentId).isEqualTo("1001");
            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.REFUNDING);
        }

        @Test
        @DisplayName("退款预处理退款金额为空时以支付金额为默认")
        void prepareRefundPhase1_nullAmount_usesAggregateAmount() {
            Payment paidAggregate = buildAggregate(PaymentStatus.SUCCESS);
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(paidAggregate));

            commandHandler.prepareRefundPhase1("1001", null);

            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.REFUNDING);
        }

        @Test
        @DisplayName("退款确认成功")
        void confirmRefundPhase2_success() {
            Payment refundingAggregate = buildAggregate(PaymentStatus.REFUNDING);
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(refundingAggregate));

            commandHandler.confirmRefundPhase2("1001", RefundResult.success("REF_123"), new BigDecimal("100.00"));

            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.REFUNDED);
            verify(domainEventPublisher).publish(any());
        }
    }

    @Nested
    @DisplayName("rollbackPayStatus 支付状态回退")
    class RollbackPayStatusTests {

        @Test
        @DisplayName("PAYING 状态回退到 PENDING")
        void rollbackPayStatus_success() {
            Payment payingAggregate = buildAggregate(PaymentStatus.PAYING);
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(payingAggregate));

            commandHandler.rollbackPayStatus("1001");

            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("rollbackRefundStatus 退款状态回退")
    class RollbackRefundStatusTests {

        @Test
        @DisplayName("REFUNDING 状态回退到 SUCCESS")
        void rollbackRefundStatus_success() {
            Payment refundingAggregate = buildAggregate(PaymentStatus.REFUNDING);
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(refundingAggregate));

            commandHandler.rollbackRefundStatus("1001");

            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.SUCCESS);
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

            assertThatThrownBy(() -> commandHandler.handle(command))
                    .isInstanceOf(PaymentDomainException.class);
        }
    }

    @Nested
    @DisplayName("handle(PayCommand) - 两阶段")
    class PayFlowTests {

        @BeforeEach
        void enableLockWrapper() {
            doAnswer(invocation -> {
                invocation.getArgument(1, Runnable.class).run();
                return null;
            }).when(lockWrapper).executeWithLock(anyString(), any(Runnable.class));
        }

        @Test
        @DisplayName("支付成功 - 网关成功走完整两阶段")
        void handle_pay_success() {
            Payment payingAggregate = buildAggregate(PaymentStatus.PAYING);
            when(paymentRepository.findByPaymentNo("PAY123")).thenReturn(Optional.of(testAggregate));
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(payingAggregate));
            when(paymentGateway.pay(any())).thenReturn(PaymentResult.success("TXN_123"));

            commandHandler.handle(new PayCommand("PAY123", null, null));

            verify(paymentGateway).pay(any());
            verify(paymentRepository, times(2)).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getAllValues().get(1).status()).isEqualTo(PaymentStatus.SUCCESS);
            verify(domainEventPublisher).publish(any());
        }

        @Test
        @DisplayName("支付失败 - 网关失败回退 PENDING")
        void handle_pay_gatewayFailure_rollsBack() {
            Payment payingAggregate = buildAggregate(PaymentStatus.PAYING);
            when(paymentRepository.findByPaymentNo("PAY123")).thenReturn(Optional.of(testAggregate));
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(payingAggregate));
            when(paymentGateway.pay(any())).thenReturn(PaymentResult.failure("网关拒绝"));

            commandHandler.handle(new PayCommand("PAY123", null, null));

            verify(paymentRepository, times(2)).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getAllValues().get(1).status()).isEqualTo(PaymentStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("handle(RefundPaymentCommand) - 两阶段")
    class RefundFlowTests {

        @BeforeEach
        void enableLockWrapper() {
            doAnswer(invocation -> {
                invocation.getArgument(1, Runnable.class).run();
                return null;
            }).when(lockWrapper).executeWithLock(anyString(), any(Runnable.class));
        }

        @Test
        @DisplayName("退款成功 - 网关成功走完整两阶段")
        void handle_refund_success() {
            Payment successAggregate = buildAggregate(PaymentStatus.SUCCESS);
            Payment refundingAggregate = buildAggregate(PaymentStatus.REFUNDING);
            when(paymentRepository.findById("1001")).thenReturn(
                    Optional.of(successAggregate), Optional.of(refundingAggregate), Optional.of(refundingAggregate));
            when(paymentGateway.refund(any(), any())).thenReturn(RefundResult.success("REF_123"));

            commandHandler.handle(new RefundPaymentCommand("1001", new BigDecimal("100.00"), "用户申请"));

            verify(paymentGateway).refund(any(), any());
            verify(paymentRepository, times(2)).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getAllValues().get(1).status()).isEqualTo(PaymentStatus.REFUNDED);
            verify(domainEventPublisher).publish(any());
        }

        @Test
        @DisplayName("退款网关失败 - 回退 SUCCESS")
        void handle_refund_gatewayFailure_rollsBack() {
            Payment successAggregate = buildAggregate(PaymentStatus.SUCCESS);
            Payment refundingAggregate = buildAggregate(PaymentStatus.REFUNDING);
            when(paymentRepository.findById("1001")).thenReturn(
                    Optional.of(successAggregate), Optional.of(refundingAggregate), Optional.of(refundingAggregate));
            when(paymentGateway.refund(any(), any())).thenReturn(RefundResult.failure("网关拒绝"));

            commandHandler.handle(new RefundPaymentCommand("1001", new BigDecimal("100.00"), "用户申请"));

            verify(paymentRepository, times(2)).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getAllValues().get(1).status()).isEqualTo(PaymentStatus.SUCCESS);
        }
    }

    // ==================== Fixture ====================

    /**
     * 构造测试用聚合根 — 除 status 外其余字段固定，便于不同状态场景复用。
     */
    private static Payment buildAggregate(PaymentStatus status) {
        var spec = new PaymentReconstructSpec(
                "1001", "PAY123", "2001", "3001",
                new BigDecimal("100.00"), BigDecimal.ZERO, PaymentMethod.WECHAT,
                status, null, null, null, null, null, null, 0
        );
        return Payment.from(spec);
    }
}
