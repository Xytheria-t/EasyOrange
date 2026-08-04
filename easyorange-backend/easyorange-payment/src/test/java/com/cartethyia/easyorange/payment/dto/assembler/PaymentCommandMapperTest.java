package com.cartethyia.easyorange.payment.dto.assembler;

import com.cartethyia.easyorange.payment.adapter.inbound.web.assembler.PaymentCommandMapper;
import com.cartethyia.easyorange.payment.adapter.inbound.web.assembler.PaymentCommandMapperImpl;
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

@DisplayName("PaymentCommandMapper 测试")
class PaymentCommandMapperTest {

    private final PaymentCommandMapper mapper = new PaymentCommandMapperImpl();

    @Nested
    @DisplayName("toCreateCommand")
    class ToCreateCommandTests {

        @Test
        @DisplayName("请求转创建命令")
        void toCreateCommand_mapsRequest() {
            CreatePaymentRequest request = new CreatePaymentRequest(
                    "2001", new BigDecimal("100.00"), "WECHAT", "pwd");

            CreatePaymentCommand command = mapper.toCreateCommand(request, null);

            assertThat(command.orderId()).isEqualTo("2001");
            assertThat(command.amount()).isEqualByComparingTo("100.00");
            assertThat(command.paymentMethod()).isEqualTo("WECHAT");
            assertThat(command.payPassword()).isEqualTo("pwd");
        }
    }

    @Nested
    @DisplayName("toPayCommand")
    class ToPayCommandTests {

        @Test
        @DisplayName("回调转支付命令")
        void toPayCommand_mapsCallback() {
            PaymentCallback callback = PaymentCallback.builder()
                    .paymentNo("PAY123")
                    .transactionId("TXN_1")
                    .attach("attach")
                    .build();

            PayCommand command = mapper.toPayCommand(callback);

            assertThat(command.paymentNo()).isEqualTo("PAY123");
            assertThat(command.transactionId()).isEqualTo("TXN_1");
            assertThat(command.attach()).isEqualTo("attach");
        }
    }

    @Nested
    @DisplayName("toRefundCommand / toCloseCommand")
    class ToRefundCloseTests {

        @Test
        @DisplayName("退款请求转退款命令")
        void toRefundCommand_mapsRequest() {
            RefundRequest request = new RefundRequest("1001", new BigDecimal("50.00"), "部分退款");

            RefundPaymentCommand command = mapper.toRefundCommand("1001", request);

            assertThat(command.paymentId()).isEqualTo("1001");
            assertThat(command.refundAmount()).isEqualByComparingTo("50.00");
            assertThat(command.refundReason()).isEqualTo("部分退款");
        }

        @Test
        @DisplayName("支付 ID 转关闭命令")
        void toCloseCommand_mapsId() {
            ClosePaymentCommand command = mapper.toCloseCommand("1001");

            assertThat(command.paymentId()).isEqualTo("1001");
        }
    }
}