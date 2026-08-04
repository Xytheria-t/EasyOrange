package com.cartethyia.easyorange.payment.dto.assembler;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.payment.adapter.inbound.web.assembler.PaymentViewAssembler;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentReconstructSpec;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentViewAssembler 测试")
class PaymentViewAssemblerTest {

    private final PaymentViewAssembler assembler = new PaymentViewAssembler();

    private Payment aggregate() {
        var spec = new PaymentReconstructSpec(
                "1001", "PAY123", "2001", "3001",
                new BigDecimal("100.00"), BigDecimal.ZERO, PaymentMethod.WECHAT,
                PaymentStatus.SUCCESS, "TXN_1", "用户退款",
                LocalDateTime.of(2026, 1, 1, 10, 0), "attach",
                LocalDateTime.of(2026, 1, 1, 9, 0), LocalDateTime.of(2026, 1, 1, 9, 30), 3
        );
        return Payment.from(spec);
    }

    @Nested
    @DisplayName("toPaymentResponse")
    class ToPaymentResponseTests {

        @Test
        @DisplayName("聚合根转响应，字段映射正确")
        void toPaymentResponse_mapsFields() {
            PaymentResponse response = assembler.toPaymentResponse(aggregate());

            assertThat(response.getId()).isEqualTo("1001");
            assertThat(response.getPaymentNo()).isEqualTo("PAY123");
            assertThat(response.getOrderId()).isEqualTo("2001");
            assertThat(response.getUserId()).isEqualTo("3001");
            assertThat(response.getAmount()).isEqualByComparingTo("100.00");
            assertThat(response.getPaymentMethod()).isEqualTo("WECHAT");
            assertThat(response.getPaymentMethodDesc()).isNotBlank();
            assertThat(response.getStatus()).isEqualTo("SUCCESS");
            assertThat(response.getStatusDesc()).isNotBlank();
            assertThat(response.getTransactionId()).isEqualTo("TXN_1");
            assertThat(response.getRefundReason()).isEqualTo("用户退款");
            assertThat(response.getRefundTime()).isNotNull();
            assertThat(response.getCreateTime()).isNotNull();
            assertThat(response.getUpdateTime()).isNotNull();
        }

        @Test
        @DisplayName("聚合根为空返回 null")
        void toPaymentResponse_null_returnsNull() {
            assertThat(assembler.toPaymentResponse(null)).isNull();
        }
    }

    @Nested
    @DisplayName("toPageResult")
    class ToPageResultTests {

        @Test
        @DisplayName("分页结果转换保留分页元数据")
        void toPageResult_mapsPage() {
            PageResult<Payment> page = PageResult.of(List.of(aggregate()), 25L, 2, 10);

            PageResult<PaymentResponse> result = assembler.toPageResult(page);

            assertThat(result.records()).hasSize(1);
            assertThat(result.records().get(0).getId()).isEqualTo("1001");
            assertThat(result.total()).isEqualTo(25L);
            assertThat(result.current()).isEqualTo(2);
            assertThat(result.size()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("toPaymentResponseWithOrderInfo")
    class ToPaymentResponseWithOrderInfoTests {

        @Test
        @DisplayName("追加订单号与用户名")
        void toPaymentResponseWithOrderInfo_addsOrderInfo() {
            PaymentResponse response = assembler.toPaymentResponseWithOrderInfo(aggregate(), "ORDER_NO_1", "张三");

            assertThat(response.getOrderNo()).isEqualTo("ORDER_NO_1");
            assertThat(response.getUsername()).isEqualTo("张三");
            assertThat(response.getId()).isEqualTo("1001");
            assertThat(response.getStatus()).isEqualTo("SUCCESS");
        }
    }
}