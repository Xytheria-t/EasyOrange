package com.cartethyia.easyorange.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.payment.application.port.query.PaymentQueryRepository;
import com.cartethyia.easyorange.payment.application.query.PaymentListQuery;
import com.cartethyia.easyorange.payment.application.query.PaymentQueryHandler;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentReconstructSpec;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentQueryHandler 测试")
class PaymentQueryHandlerTest {

    @Mock
    private PaymentQueryRepository paymentQueryRepository;

    @InjectMocks
    private PaymentQueryHandler queryHandler;

    private static final String USER_ID = "3001";

    private Payment createTestAggregate(String id, String paymentNo, PaymentStatus status) {
        var spec = new PaymentReconstructSpec(
                id,
                paymentNo,
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
    @DisplayName("getPaymentById")
    class GetByIdTests {

        @Test
        @DisplayName("查询本人支付记录成功")
        void getById_found() {
            Payment aggregate = createTestAggregate("1001", "PAY123", PaymentStatus.SUCCESS);
            when(paymentQueryRepository.findAggregateById("1001")).thenReturn(Optional.of(aggregate));

            Payment result = queryHandler.getPaymentById("1001", USER_ID);

            assertThat(result.id()).isEqualTo("1001");
            assertThat(result.paymentNo()).isEqualTo("PAY123");
            assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("支付记录不存在抛出异常")
        void getById_notFound() {
            when(paymentQueryRepository.findAggregateById("9999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryHandler.getPaymentById("9999", USER_ID))
                    .isInstanceOf(PaymentDomainException.class);
        }

        @Test
        @DisplayName("越权查询他人支付单 - 按记录不存在拒绝")
        void getById_ownershipMismatch_rejected() {
            Payment aggregate = createTestAggregate("1001", "PAY123", PaymentStatus.SUCCESS);
            when(paymentQueryRepository.findAggregateById("1001")).thenReturn(Optional.of(aggregate));

            assertThatThrownBy(() -> queryHandler.getPaymentById("1001", "9999"))
                    .isInstanceOf(PaymentDomainException.class)
                    .satisfies(e -> assertThat(((PaymentDomainException) e).getCode())
                            .isEqualTo(PaymentResultCode.PAYMENT_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("getPaymentByOrderId")
    class GetByOrderIdTests {

        @Test
        @DisplayName("按订单ID查询本人支付记录成功")
        void getByOrderId_found() {
            Payment aggregate = createTestAggregate("1001", "PAY123", PaymentStatus.PENDING);
            when(paymentQueryRepository.findAggregateByOrderId("2001")).thenReturn(Optional.of(aggregate));

            Payment result = queryHandler.getPaymentByOrderId("2001", USER_ID);

            assertThat(result.orderId()).isEqualTo("2001");
        }

        @Test
        @DisplayName("越权按订单ID查询他人支付单 - 按记录不存在拒绝")
        void getByOrderId_ownershipMismatch_rejected() {
            Payment aggregate = createTestAggregate("1001", "PAY123", PaymentStatus.PENDING);
            when(paymentQueryRepository.findAggregateByOrderId("2001")).thenReturn(Optional.of(aggregate));

            assertThatThrownBy(() -> queryHandler.getPaymentByOrderId("2001", "9999"))
                    .isInstanceOf(PaymentDomainException.class);
        }
    }

    @Nested
    @DisplayName("getMyPayments")
    class GetMyPaymentsTests {

        @Test
        @DisplayName("自动填充当前登录用户")
        void getMyPayments_usesCurrentUser() {
            Payment aggregate = createTestAggregate("1001", "PAY123", PaymentStatus.SUCCESS);
            when(paymentQueryRepository.findByUserIdAndStatus("3001", null, 1, 20))
                    .thenReturn(List.of(aggregate));
            when(paymentQueryRepository.countByUserIdAndStatus("3001", null)).thenReturn(1L);

            var result = queryHandler.getMyPayments(USER_ID, new PaymentListQuery(null, null, null, null));

            assertThat(result.records()).hasSize(1);
            assertThat(result.total()).isEqualTo(1L);
            assertThat(result.current()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("queryPayments")
    class QueryPaymentsTests {

        @Test
        @DisplayName("按用户与状态过滤")
        void queryPayments_filtersByUserAndStatus() {
            Payment aggregate = createTestAggregate("1001", "PAY123", PaymentStatus.REFUNDED);
            when(paymentQueryRepository.findByUserIdAndStatus("3001", PaymentStatus.REFUNDED, 2, 10))
                    .thenReturn(List.of(aggregate));
            when(paymentQueryRepository.countByUserIdAndStatus("3001", PaymentStatus.REFUNDED))
                    .thenReturn(5L);

            var result = queryHandler.queryPayments(new PaymentListQuery("3001", PaymentStatus.REFUNDED, 2, 10));

            assertThat(result.records()).hasSize(1);
            assertThat(result.total()).isEqualTo(5L);
            assertThat(result.current()).isEqualTo(2);
        }
    }
}
