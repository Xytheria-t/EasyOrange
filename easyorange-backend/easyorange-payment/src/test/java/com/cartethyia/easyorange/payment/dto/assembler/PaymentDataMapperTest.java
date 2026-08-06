package com.cartethyia.easyorange.payment.dto.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.payment.adapter.outbound.persistence.PaymentDO;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.converter.PaymentDataMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.converter.PaymentDataMapperImpl;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentDataMapper 测试")
class PaymentDataMapperTest {

    private final PaymentDataMapper mapper = new PaymentDataMapperImpl();

    @Nested
    @DisplayName("toAggregate")
    class ToAggregateTests {

        @Test
        @DisplayName("空值字段回退为默认值")
        void toAggregate_nullRefundedAmount_defaultsToZero() {
            PaymentDO po = PaymentDO.builder()
                    .id("1001")
                    .paymentNo("PAY123")
                    .orderId("2001")
                    .userId("3001")
                    .amount(new BigDecimal("100.00"))
                    .paymentMethod(PaymentMethod.WECHAT)
                    .status(PaymentStatus.SUCCESS)
                    .transactionId("TXN_1")
                    .version(3)
                    .build();

            Payment aggregate = mapper.toAggregate(po);

            assertThat(aggregate.id()).isEqualTo("1001");
            assertThat(aggregate.paymentNo()).isEqualTo("PAY123");
            assertThat(aggregate.orderId()).isEqualTo("2001");
            assertThat(aggregate.userId()).isEqualTo("3001");
            assertThat(aggregate.refundedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(aggregate.paymentMethod()).isEqualTo(PaymentMethod.WECHAT);
            assertThat(aggregate.status()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(aggregate.transactionId()).isEqualTo("TXN_1");
            assertThat(aggregate.version()).isEqualTo(3);
        }

        @Test
        @DisplayName("非空字段透传")
        void toAggregate_fullFields_passthrough() {
            PaymentDO po = PaymentDO.builder()
                    .id("1001")
                    .paymentNo("PAY123")
                    .orderId("2001")
                    .userId("3001")
                    .amount(new BigDecimal("100.00"))
                    .refundedAmount(new BigDecimal("50.00"))
                    .paymentMethod(PaymentMethod.ALIPAY)
                    .status(PaymentStatus.REFUNDED)
                    .transactionId("TXN_1")
                    .refundReason("用户退款")
                    .refundTime(LocalDateTime.of(2026, 1, 1, 10, 0))
                    .attach("attach")
                    .createTime(LocalDateTime.of(2026, 1, 1, 9, 0))
                    .updateTime(LocalDateTime.of(2026, 1, 1, 9, 30))
                    .version(2)
                    .build();

            Payment aggregate = mapper.toAggregate(po);

            assertThat(aggregate.refundedAmount()).isEqualByComparingTo("50.00");
            assertThat(aggregate.refundReason()).isEqualTo("用户退款");
            assertThat(aggregate.refundTime()).isNotNull();
            assertThat(aggregate.attach()).isEqualTo("attach");
            assertThat(aggregate.createTime()).isNotNull();
            assertThat(aggregate.updateTime()).isNotNull();
        }

        @Test
        @DisplayName("PO 为空返回 null")
        void toAggregate_null_returnsNull() {
            assertThat(mapper.toAggregate(null)).isNull();
        }
    }

    @Nested
    @DisplayName("toPO")
    class ToPOTests {

        @Test
        @DisplayName("聚合根转 PO，字段映射正确")
        void toPO_mapsFields() {
            var spec = new com.cartethyia.easyorange.payment.domain.aggregate.PaymentReconstructSpec(
                    "1001",
                    "PAY123",
                    "2001",
                    "3001",
                    new BigDecimal("100.00"),
                    new BigDecimal("20.00"),
                    PaymentMethod.WECHAT,
                    PaymentStatus.PARTIALLY_REFUNDED,
                    "TXN_1",
                    "部分退款",
                    LocalDateTime.of(2026, 1, 1, 10, 0),
                    "attach",
                    LocalDateTime.of(2026, 1, 1, 9, 0),
                    LocalDateTime.of(2026, 1, 1, 9, 30),
                    4);
            Payment aggregate = Payment.from(spec);

            PaymentDO po = mapper.toPO(aggregate);

            assertThat(po.getId()).isEqualTo("1001");
            assertThat(po.getPaymentNo()).isEqualTo("PAY123");
            assertThat(po.getOrderId()).isEqualTo("2001");
            assertThat(po.getUserId()).isEqualTo("3001");
            assertThat(po.getAmount()).isEqualByComparingTo("100.00");
            assertThat(po.getRefundedAmount()).isEqualByComparingTo("20.00");
            assertThat(po.getPaymentMethod()).isEqualTo(PaymentMethod.WECHAT);
            assertThat(po.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
            assertThat(po.getTransactionId()).isEqualTo("TXN_1");
            assertThat(po.getRefundReason()).isEqualTo("部分退款");
            assertThat(po.getAttach()).isEqualTo("attach");
            assertThat(po.getVersion()).isEqualTo(4);
        }

        @Test
        @DisplayName("聚合根为空返回 null")
        void toPO_null_returnsNull() {
            assertThat(mapper.toPO(null)).isNull();
        }
    }
}
