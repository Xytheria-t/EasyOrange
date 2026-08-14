package com.cartethyia.easyorange.payment.adapter.inbound.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.MockPaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentReconstructSpec;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MockPaymentController 测试")
class MockPaymentControllerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private IdGenerator idGenerator;

    private MockPaymentController controller;

    @BeforeEach
    void setUp() {
        controller = new MockPaymentController(paymentRepository, idGenerator);
    }

    private Payment aggregate(PaymentStatus status) {
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

    @Nested
    @DisplayName("createMockPayment")
    class CreateMockTests {

        @Test
        @DisplayName("创建模拟支付并保存")
        void createMockPayment_savesAggregate() {
            when(idGenerator.generateId()).thenReturn("1001");

            Result<PaymentResponse> result = controller.createMockPayment("2001", "WECHAT", new BigDecimal("100.00"));

            verify(paymentRepository).save(any(Payment.class));
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data().getId()).isEqualTo("1001");
        }
    }

    @Nested
    @DisplayName("processMockPayment")
    class ProcessMockTests {

        @Test
        @DisplayName("成功标记 - 进入成功状态")
        void processMockPayment_success_confirms() {
            when(paymentRepository.findById("1001"))
                    .thenReturn(
                            Optional.of(aggregate(PaymentStatus.PENDING)),
                            Optional.of(aggregate(PaymentStatus.SUCCESS)));

            Result<PaymentResponse> result =
                    controller.processMockPayment(new MockPaymentRequest("1001", null, null, null, true));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data().getStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("失败标记 - 进入失败状态")
        void processMockPayment_failure_marksFailed() {
            when(paymentRepository.findById("1001"))
                    .thenReturn(
                            Optional.of(aggregate(PaymentStatus.PENDING)),
                            Optional.of(aggregate(PaymentStatus.FAILED)));

            Result<PaymentResponse> result =
                    controller.processMockPayment(new MockPaymentRequest("1001", null, null, null, false));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data().getStatus()).isEqualTo("FAILED");
        }
    }

    @Nested
    @DisplayName("mockPaymentSuccess / mockPaymentFail")
    class MockOutcomeTests {

        @Test
        @DisplayName("模拟支付成功")
        void mockPaymentSuccess_confirms() {
            when(paymentRepository.findById("1001"))
                    .thenReturn(
                            Optional.of(aggregate(PaymentStatus.PENDING)),
                            Optional.of(aggregate(PaymentStatus.SUCCESS)));

            Result<PaymentResponse> result = controller.mockPaymentSuccess("1001");

            assertThat(result.data().getStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("模拟支付失败")
        void mockPaymentFail_marksFailed() {
            when(paymentRepository.findById("1001"))
                    .thenReturn(
                            Optional.of(aggregate(PaymentStatus.PENDING)),
                            Optional.of(aggregate(PaymentStatus.FAILED)));

            Result<PaymentResponse> result = controller.mockPaymentFail("1001");

            assertThat(result.data().getStatus()).isEqualTo("FAILED");
        }
    }

    @Nested
    @DisplayName("mockRefund")
    class MockRefundTests {

        @Test
        @DisplayName("模拟直接退款")
        void mockRefund_directRefund() {
            when(paymentRepository.findById("1001")).thenReturn(Optional.of(aggregate(PaymentStatus.SUCCESS)));

            Result<Void> result = controller.mockRefund("1001", "用户申请");

            verify(paymentRepository).update(any());
            assertThat(result.isSuccess()).isTrue();
        }
    }
}
