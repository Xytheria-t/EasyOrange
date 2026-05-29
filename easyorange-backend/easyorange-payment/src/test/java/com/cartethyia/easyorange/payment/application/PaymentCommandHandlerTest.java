package com.cartethyia.easyorange.payment.application;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.payment.application.command.ClosePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.exception.PaymentNotFoundException;
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

    @InjectMocks
    private PaymentCommandHandler commandHandler;

    @Captor
    private ArgumentCaptor<PaymentAggregate> aggregateCaptor;

    private PaymentAggregate testAggregate;

    @BeforeEach
    void setUp() {
        TestSecurityUtil.setSecurityContext(3001L);

        testAggregate = PaymentAggregate.reconstruct(
                1001L, "PAY123", 2001L, 3001L,
                new BigDecimal("100.00"), BigDecimal.ZERO, 1,
                PaymentStatus.PENDING, null, null, null, null, null, null, 0
        );
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
            CreatePaymentCommand command = CreatePaymentCommand.builder()
                    .orderId(2001L)
                    .amount(new BigDecimal("100.00"))
                    .paymentMethod(1)
                    .attach("test")
                    .build();

            Long paymentId = commandHandler.handle(command);

            assertThat(paymentId).isNotNull();
            verify(paymentRepository).save(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().orderId()).isEqualTo(2001L);
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

            Long paymentId = commandHandler.preparePayPhase1("PAY123");

            assertThat(paymentId).isEqualTo(1001L);
            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.PAYING);
        }

        @Test
        @DisplayName("支付成功 - 阶段2确认")
        void confirmPayPhase2_success() {
            PaymentAggregate payingAggregate = PaymentAggregate.reconstruct(
                    1001L, "PAY123", 2001L, 3001L,
                    new BigDecimal("100.00"), BigDecimal.ZERO, 1,
                    PaymentStatus.PAYING, null, null, null, null, null, null, 0
            );
            when(paymentRepository.findById(1001L)).thenReturn(Optional.of(payingAggregate));

            commandHandler.confirmPayPhase2(1001L, PaymentResult.success("TXN_123"));

            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.SUCCESS);
            verify(domainEventPublisher).publish(any());
        }

        @Test
        @DisplayName("支付记录不存在抛出异常")
        void handle_pay_notFound() {
            when(paymentRepository.findByPaymentNo("NOT_EXIST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commandHandler.preparePayPhase1("NOT_EXIST"))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("handle(RefundPaymentCommand) - 两阶段")
    class RefundCommandTests {

        @Test
        @DisplayName("退款预处理成功")
        void prepareRefundPhase1_success() {
            PaymentAggregate paidAggregate = PaymentAggregate.reconstruct(
                    1001L, "PAY123", 2001L, 3001L,
                    new BigDecimal("100.00"), BigDecimal.ZERO, 1,
                    PaymentStatus.SUCCESS, null, null, null, null, null, null, 0
            );
            when(paymentRepository.findById(1001L)).thenReturn(Optional.of(paidAggregate));

            Long paymentId = commandHandler.prepareRefundPhase1(1001L, new BigDecimal("100.00"));

            assertThat(paymentId).isEqualTo(1001L);
            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.REFUNDING);
        }

        @Test
        @DisplayName("退款确认成功")
        void confirmRefundPhase2_success() {
            PaymentAggregate refundingAggregate = PaymentAggregate.reconstruct(
                    1001L, "PAY123", 2001L, 3001L,
                    new BigDecimal("100.00"), BigDecimal.ZERO, 1,
                    PaymentStatus.REFUNDING, null, null, null, null, null, null, 0
            );
            when(paymentRepository.findById(1001L)).thenReturn(Optional.of(refundingAggregate));

            commandHandler.confirmRefundPhase2(1001L, RefundResult.success("REF_123"), new BigDecimal("100.00"));

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
            PaymentAggregate payingAggregate = PaymentAggregate.reconstruct(
                    1001L, "PAY123", 2001L, 3001L,
                    new BigDecimal("100.00"), BigDecimal.ZERO, 1,
                    PaymentStatus.PAYING, null, null, null, null, null, null, 0
            );
            when(paymentRepository.findById(1001L)).thenReturn(Optional.of(payingAggregate));

            commandHandler.rollbackPayStatus(1001L);

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
            PaymentAggregate refundingAggregate = PaymentAggregate.reconstruct(
                    1001L, "PAY123", 2001L, 3001L,
                    new BigDecimal("100.00"), BigDecimal.ZERO, 1,
                    PaymentStatus.REFUNDING, null, null, null, null, null, null, 0
            );
            when(paymentRepository.findById(1001L)).thenReturn(Optional.of(refundingAggregate));

            commandHandler.rollbackRefundStatus(1001L);

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
            when(paymentRepository.findById(1001L)).thenReturn(Optional.of(testAggregate));

            ClosePaymentCommand command = ClosePaymentCommand.builder()
                    .paymentId(1001L)
                    .build();

            commandHandler.handle(command);

            verify(paymentRepository).update(aggregateCaptor.capture());
            assertThat(aggregateCaptor.getValue().status()).isEqualTo(PaymentStatus.CLOSED);
            verify(domainEventPublisher).publish(any());
        }

        @Test
        @DisplayName("支付记录不存在抛出异常")
        void handle_close_notFound() {
            when(paymentRepository.findById(9999L)).thenReturn(Optional.empty());

            ClosePaymentCommand command = ClosePaymentCommand.builder()
                    .paymentId(9999L)
                    .build();

            assertThatThrownBy(() -> commandHandler.handle(command))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }
}
