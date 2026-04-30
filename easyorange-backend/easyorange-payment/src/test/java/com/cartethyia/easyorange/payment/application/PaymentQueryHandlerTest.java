package com.cartethyia.easyorange.payment.application;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.payment.application.query.PaymentQueryHandler;
import com.cartethyia.easyorange.payment.application.query.PaymentView;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.repository.PaymentQueryRepository;
import com.cartethyia.easyorange.payment.enums.PaymentStatus;
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
    private PaymentQueryRepository paymentQueryRepository;

    @InjectMocks
    private PaymentQueryHandler queryHandler;

    private PaymentAggregate createTestAggregate(Long id, String paymentNo, PaymentStatus status) {
        return PaymentAggregate.reconstruct(
                id, paymentNo, 2001L, 3001L,
                new BigDecimal("100.00"), BigDecimal.ZERO, 1,
                status, null, null, null, null, null, null
        );
    }

    @Nested
    @DisplayName("getPaymentById")
    class GetByIdTests {

        @Test
        @DisplayName("查询支付记录成功")
        void getById_found() {
            PaymentAggregate aggregate = createTestAggregate(1001L, "PAY123", PaymentStatus.SUCCESS);
            when(paymentQueryRepository.findAggregateById(1001L)).thenReturn(Optional.of(aggregate));

            PaymentView view = queryHandler.getPaymentById(1001L);

            assertThat(view.getId()).isEqualTo(1001L);
            assertThat(view.getPaymentNo()).isEqualTo("PAY123");
            assertThat(view.getStatus()).isEqualTo(PaymentStatus.SUCCESS.getCode());
        }

        @Test
        @DisplayName("支付记录不存在抛出异常")
        void getById_notFound() {
            when(paymentQueryRepository.findAggregateById(9999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> queryHandler.getPaymentById(9999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("getPaymentByOrderId")
    class GetByOrderIdTests {

        @Test
        @DisplayName("按订单ID查询支付记录成功")
        void getByOrderId_found() {
            PaymentAggregate aggregate = createTestAggregate(1001L, "PAY123", PaymentStatus.PENDING);
            when(paymentQueryRepository.findAggregateByOrderId(2001L)).thenReturn(Optional.of(aggregate));

            PaymentView view = queryHandler.getPaymentByOrderId(2001L);

            assertThat(view.getOrderId()).isEqualTo(2001L);
        }
    }
}
