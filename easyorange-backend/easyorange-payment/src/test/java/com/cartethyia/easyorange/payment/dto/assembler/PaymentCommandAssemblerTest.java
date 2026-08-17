package com.cartethyia.easyorange.payment.adapter.inbound.web.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.payment.adapter.inbound.web.request.CreatePaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.PaymentCallback;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.RefundRequest;
import com.cartethyia.easyorange.payment.application.command.ClosePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCallbackCommand;
import com.cartethyia.easyorange.payment.application.command.RefundPaymentCommand;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentCommandMapper 测试")
class PaymentCommandAssemblerTest {

    private final PaymentCommandMapper mapper = new PaymentCommandMapper() {};

    @Nested
    @DisplayName("toCreateCommand")
    class ToCreateCommandTests {

        @Test
        @DisplayName("正确转换 CreatePaymentRequest 到 CreatePaymentCommand")
        void toCreateCommand_convertsCorrectly() {
            CreatePaymentRequest request = new CreatePaymentRequest();
            request.setOrderId("1001");
            request.setPaymentMethod("1");
            request.setPayPassword("123456");

            CreatePaymentCommand command = mapper.toCreateCommand(request, "2001");

            assertThat(command.orderId()).isEqualTo("1001");
            assertThat(command.paymentMethod()).isEqualTo("1");
            assertThat(command.attach()).isNull();
        }
    }

    @Nested
    @DisplayName("toCallbackCommand")
    class ToCallbackCommandTests {

        @Test
        @DisplayName("正确转换 PaymentCallback 到 PaymentCallbackCommand")
        void toCallbackCommand_convertsCorrectly() {
            PaymentCallback callback = PaymentCallback.builder()
                    .paymentNo("PAY123")
                    .transactionId("TXN456")
                    .amount(new BigDecimal("100.00"))
                    .attach("test_attach")
                    .build();

            PaymentCallbackCommand command = mapper.toCallbackCommand(callback);

            assertThat(command.paymentNo()).isEqualTo("PAY123");
            assertThat(command.transactionId()).isEqualTo("TXN456");
            assertThat(command.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }
    }

    @Nested
    @DisplayName("toRefundCommand")
    class ToRefundCommandTests {

        @Test
        @DisplayName("正确转换 RefundRequest 到 RefundPaymentCommand（含操作者）")
        void toRefundCommand_convertsCorrectly() {
            RefundRequest request = new RefundRequest();
            request.setPaymentId("1001");
            request.setRefundAmount(new BigDecimal("50.00"));
            request.setRefundReason("测试退款");

            RefundPaymentCommand command = mapper.toRefundCommand("2001", "3001", request);

            assertThat(command.paymentId()).isEqualTo("2001");
            assertThat(command.userId()).isEqualTo("3001");
            assertThat(command.refundAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(command.refundReason()).isEqualTo("测试退款");
        }
    }

    @Nested
    @DisplayName("toCloseCommand")
    class ToCloseCommandTests {

        @Test
        @DisplayName("正确转换 paymentId 到 ClosePaymentCommand（含操作者）")
        void toCloseCommand_convertsCorrectly() {
            ClosePaymentCommand command = mapper.toCloseCommand("1001", "3001");

            assertThat(command.paymentId()).isEqualTo("1001");
            assertThat(command.userId()).isEqualTo("3001");
        }
    }
}
