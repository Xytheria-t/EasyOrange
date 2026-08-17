package com.cartethyia.easyorange.adapter.outbound.payment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.order.domain.constant.OrderResultCode;
import com.cartethyia.easyorange.payment.application.command.PayCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.application.command.RefundPaymentCommand;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentReconstructSpec;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderPaymentGatewayAdapter 单元测试")
class OrderPaymentGatewayAdapterTest {

    @Mock
    private PaymentCommandHandler paymentCommandHandler;

    @Mock
    private PaymentRepository paymentRepository;

    private OrderPaymentGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OrderPaymentGatewayAdapter(paymentCommandHandler, paymentRepository);
    }

    private Payment payment() {
        var spec = new PaymentReconstructSpec(
                "1001",
                "PAY123",
                "2001",
                "3001",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                PaymentMethod.WECHAT,
                PaymentStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                null,
                0);
        return Payment.from(spec);
    }

    @Test
    @DisplayName("发起支付 - 按 orderId 解析支付单并委托两阶段支付")
    void pay_resolvesPaymentAndDelegates() {
        when(paymentRepository.findByOrderId("2001")).thenReturn(Optional.of(payment()));

        adapter.pay("2001");

        verify(paymentCommandHandler)
                .handle(argThat((PayCommand cmd) -> cmd.paymentNo().equals("PAY123")));
    }

    @Test
    @DisplayName("支付单不存在时抛出订单业务异常")
    void pay_paymentNotFound_throws() {
        when(paymentRepository.findByOrderId("2001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.pay("2001"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(OrderResultCode.ORDER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("订单取消自动退款 - 操作者记为支付单所属用户（通过归属校验）")
    void refundPayment_passesPaymentOwnerAsOperator() {
        when(paymentRepository.findByOrderId("2001")).thenReturn(Optional.of(payment()));

        adapter.refundPayment("2001", "订单取消");

        verify(paymentCommandHandler)
                .handle(argThat((RefundPaymentCommand cmd) -> cmd.paymentId().equals("1001")
                        && cmd.userId().equals("3001")
                        && cmd.refundAmount().compareTo(new BigDecimal("100.00")) == 0
                        && cmd.refundReason().equals("订单取消")));
    }
}
