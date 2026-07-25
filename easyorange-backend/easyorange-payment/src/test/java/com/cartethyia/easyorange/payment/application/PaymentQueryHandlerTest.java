package com.cartethyia.easyorange.payment.application;

import com.cartethyia.easyorange.payment.application.query.PaymentQueryHandler;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentReconstructSpec;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.port.PaymentQueryRepositoryPort;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentQueryHandler 测试")
class PaymentQueryHandlerTest {

    @Mock
    private PaymentQueryRepositoryPort paymentQueryRepository;

    @InjectMocks
    private PaymentQueryHandler queryHandler;

    private PaymentAggregate createTestAggregate(String id, String paymentNo, PaymentStatus status) {
        var spec = new PaymentReconstructSpec(
                id, paymentNo, "2001", "3001",
                new BigDecimal("100.00"), BigDecimal.ZERO, PaymentMethod.WECHAT,
                status, null, null, null, null, null, null, 0
        );
        return PaymentAggregate.from(spec);
    }

    @Nested
    @DisplayName("getPaymentById")
    class GetByIdTests {

        @Test
        @DisplayName("查询支付记录成功")
        void getById_found() {
            PaymentAggregate aggregate = createTestAggregate("1001", "PAY123", PaymentStatus.SUCCESS);
            when(paymentQueryRepository.findAggregateById("1001")).thenReturn(Optional.of(aggregate));

            PaymentAggregate result = queryHandler.getPaymentById("1001");

            assertThat(result.id()).isEqualTo("1001");
            assertThat(result.paymentNo()).isEqualTo("PAY123");
            assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("支付记录不存在抛出异常")
        void getById_notFound() {
            when(paymentQueryRepository.findAggregateById("9999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryHandler.getPaymentById("9999"))
                    .isInstanceOf(PaymentDomainException.class);
        }
    }

    @Nested
    @DisplayName("getPaymentByOrderId")
    class GetByOrderIdTests {

        @Test
        @DisplayName("按订单ID查询支付记录成功")
        void getByOrderId_found() {
            PaymentAggregate aggregate = createTestAggregate("1001", "PAY123", PaymentStatus.PENDING);
            when(paymentQueryRepository.findAggregateByOrderId("2001")).thenReturn(Optional.of(aggregate));

            PaymentAggregate result = queryHandler.getPaymentByOrderId("2001");

            assertThat(result.orderId()).isEqualTo("2001");
        }
    }
}
