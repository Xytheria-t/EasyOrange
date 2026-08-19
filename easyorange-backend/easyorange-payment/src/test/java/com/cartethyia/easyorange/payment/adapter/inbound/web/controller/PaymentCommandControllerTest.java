package com.cartethyia.easyorange.payment.adapter.inbound.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.CreatePaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.PaymentCallback;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.RefundRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.application.command.ClosePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCallbackCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.application.command.RefundPaymentCommand;
import com.cartethyia.easyorange.payment.domain.port.CallbackSignatureVerifierPort;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCommandController 测试")
class PaymentCommandControllerTest {

    @Mock
    private PaymentCommandHandler commandHandler;

    @Mock
    private CallbackSignatureVerifierPort signatureVerifier;

    private PaymentCommandController controller;

    private static final String USER_ID = "3001";

    private static AuthUser currentUser() {
        return new AuthUser(USER_ID, "tester");
    }

    @BeforeEach
    void setUp() {
        controller = new PaymentCommandController(commandHandler, signatureVerifier);
    }

    @Nested
    @DisplayName("createPayment")
    class CreatePaymentTests {

        @Test
        @DisplayName("创建支付成功返回支付 ID")
        void createPayment_success() {
            CreatePaymentRequest request = new CreatePaymentRequest("2001", new BigDecimal("100.00"), "WECHAT", null);
            when(commandHandler.handle(eq(USER_ID), any(CreatePaymentCommand.class)))
                    .thenReturn("1001");

            Result<PaymentResponse> result = controller.createPayment(currentUser(), request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.data().getId()).isEqualTo("1001");
        }
    }

    @Nested
    @DisplayName("paymentCallback")
    class PaymentCallbackTests {

        @Test
        @DisplayName("回调验签通过后直接确认支付成功")
        void paymentCallback_verifiesAndHandles() {
            PaymentCallback callback = PaymentCallback.builder()
                    .paymentNo("PAY123")
                    .transactionId("TXN_1")
                    .sign("sign")
                    .build();

            Result<Void> result = controller.paymentCallback(callback);

            verify(signatureVerifier).verify("PAY123", "TXN_1", "sign");
            verify(commandHandler).handle(any(PaymentCallbackCommand.class));
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("refund / close")
    class RefundCloseTests {

        @Test
        @DisplayName("退款成功（操作者身份透传）")
        void refund_success() {
            RefundRequest request = new RefundRequest("1001", new BigDecimal("100.00"), "用户申请");

            Result<Void> result = controller.refund(currentUser(), "1001", request);

            verify(commandHandler)
                    .handle(argThat((RefundPaymentCommand cmd) ->
                            cmd.paymentId().equals("1001") && cmd.userId().equals(USER_ID)));
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("关闭支付成功（操作者身份透传）")
        void close_success() {
            Result<Void> result = controller.close(currentUser(), "1001");

            verify(commandHandler)
                    .handle(argThat((ClosePaymentCommand cmd) ->
                            cmd.paymentId().equals("1001") && cmd.userId().equals(USER_ID)));
            assertThat(result.isSuccess()).isTrue();
        }
    }
}
