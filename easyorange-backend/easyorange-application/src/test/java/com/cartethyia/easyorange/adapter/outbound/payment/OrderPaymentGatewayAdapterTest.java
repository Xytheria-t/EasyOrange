package com.cartethyia.easyorange.adapter.outbound.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.order.domain.port.PaymentGatewayPort.CreatePaymentRequest;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 适配器只做「订单侧 orderId 键 → 支付模块用例」的翻译与委托，断言因此只覆盖两件事：
 * 委托参数正确，以及支付模块的异常原样穿透（不再被换成订单侧错误码）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderPaymentGatewayAdapter 单元测试")
class OrderPaymentGatewayAdapterTest {

    @Mock
    private PaymentCommandHandler paymentCommandHandler;

    private OrderPaymentGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OrderPaymentGatewayAdapter(paymentCommandHandler);
    }

    @Test
    @DisplayName("创建支付 - 订单侧请求翻译为支付命令并按买家身份提交")
    void createPayment_translatesRequestToCommand() {
        when(paymentCommandHandler.createPayment(eq("3001"), any(CreatePaymentCommand.class)))
                .thenReturn("1001");

        String paymentId = adapter.createPayment(
                new CreatePaymentRequest("2001", new BigDecimal("100.00"), "WECHAT", "ATTACH", "资产描述", "3001"));

        assertThat(paymentId).isEqualTo("1001");
        verify(paymentCommandHandler)
                .createPayment(
                        eq("3001"),
                        argThat((CreatePaymentCommand cmd) -> cmd.orderId().equals("2001")
                                && cmd.amount().compareTo(new BigDecimal("100.00")) == 0
                                && cmd.paymentMethod().equals("WECHAT")
                                && cmd.attach().equals("ATTACH")));
    }

    @Test
    @DisplayName("发起支付 - 以 orderId 委托支付模块用例")
    void pay_delegatesByOrderId() {
        adapter.pay("2001");

        verify(paymentCommandHandler).payByOrderId("2001");
    }

    @Test
    @DisplayName("退款 - 以 orderId 与原因委托支付模块用例")
    void refundPayment_delegatesByOrderId() {
        adapter.refundPayment("2001", "订单取消");

        verify(paymentCommandHandler).refundByOrderId("2001", "订单取消");
    }

    @Test
    @DisplayName("支付单不存在 - 支付模块的 B4001 原样穿透，不被替换为订单侧错误码")
    void pay_paymentNotFound_propagatesPaymentModuleCode() {
        doThrow(PaymentDomainException.notFound("orderId=2001"))
                .when(paymentCommandHandler)
                .payByOrderId("2001");

        assertThatThrownBy(() -> adapter.pay("2001"))
                .isInstanceOf(PaymentDomainException.class)
                .extracting(e -> ((PaymentDomainException) e).getCode())
                .isEqualTo(PaymentResultCode.PAYMENT_NOT_FOUND.getCode());
    }
}
