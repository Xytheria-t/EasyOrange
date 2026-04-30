package com.cartethyia.easyorange.payment.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.payment.adapter.inbound.web.request.CreatePaymentRequest;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.PaymentCallback;
import com.cartethyia.easyorange.payment.adapter.inbound.web.request.RefundRequest;
import com.cartethyia.easyorange.payment.application.command.ClosePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.CreatePaymentCommand;
import com.cartethyia.easyorange.payment.application.command.PayCommand;
import com.cartethyia.easyorange.payment.application.command.RefundPaymentCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentCommandAssembler 测试")
class PaymentCommandAssemblerTest {

    @Nested
    @DisplayName("toCreateCommand")
    class ToCreateCommandTests {

        @Test
        @DisplayName("正确转换 CreatePaymentRequest 到 CreatePaymentCommand")
        void toCreateCommand_convertsCorrectly() {
            CreatePaymentRequest request = new CreatePaymentRequest();
            request.setOrderId(1001L);
            request.setPaymentMethod(1);
            request.setPayPassword("123456");

            CreatePaymentCommand command = PaymentCommandAssembler.toCreateCommand(request, 2001L);

            assertThat(command.getOrderId()).isEqualTo(1001L);
            assertThat(command.getPaymentMethod()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("toPayCommand")
    class ToPayCommandTests {

        @Test
        @DisplayName("正确转换 PaymentCallback 到 PayCommand")
        void toPayCommand_convertsCorrectly() {
            PaymentCallback callback = PaymentCallback.builder()
                    .paymentNo("PAY123")
                    .transactionId("TXN456")
                    .attach("test_attach")
                    .build();

            PayCommand command = PaymentCommandAssembler.toPayCommand(callback);

            assertThat(command.getPaymentNo()).isEqualTo("PAY123");
            assertThat(command.getTransactionId()).isEqualTo("TXN456");
            assertThat(command.getAttach()).isEqualTo("test_attach");
        }
    }

    @Nested
    @DisplayName("toRefundCommand")
    class ToRefundCommandTests {

        @Test
        @DisplayName("正确转换 RefundRequest 到 RefundPaymentCommand")
        void toRefundCommand_convertsCorrectly() {
            RefundRequest request = new RefundRequest();
            request.setPaymentId(1001L);
            request.setRefundAmount(new BigDecimal("50.00"));
            request.setRefundReason("测试退款");

            RefundPaymentCommand command = PaymentCommandAssembler.toRefundCommand(2001L, request);

            assertThat(command.getPaymentId()).isEqualTo(2001L);
            assertThat(command.getRefundAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(command.getRefundReason()).isEqualTo("测试退款");
        }
    }

    @Nested
    @DisplayName("toCloseCommand")
    class ToCloseCommandTests {

        @Test
        @DisplayName("正确转换 paymentId 到 ClosePaymentCommand")
        void toCloseCommand_convertsCorrectly() {
            ClosePaymentCommand command = PaymentCommandAssembler.toCloseCommand(1001L);

            assertThat(command.getPaymentId()).isEqualTo(1001L);
        }
    }
}
