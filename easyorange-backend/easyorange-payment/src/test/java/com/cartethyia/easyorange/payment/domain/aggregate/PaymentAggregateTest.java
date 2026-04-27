package com.cartethyia.easyorange.payment.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentFailedEvent;
import com.cartethyia.easyorange.payment.domain.event.PaymentSucceededEvent;
import com.cartethyia.easyorange.payment.domain.strategy.MockPaymentStrategy;
import com.cartethyia.easyorange.payment.domain.strategy.PaymentResult;
import com.cartethyia.easyorange.payment.domain.strategy.RefundResult;
import com.cartethyia.easyorange.payment.entity.Payment;
import com.cartethyia.easyorange.payment.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentAggregate 聚合根测试")
class PaymentAggregateTest {

    private final MockPaymentStrategy mockStrategy = new MockPaymentStrategy();

    @Nested
    @DisplayName("create 静态工厂方法")
    class CreateTests {

        @Test
        @DisplayName("创建支付成功")
        void create_withValidParams_createsPayment() {
            PaymentCreatedEvent event = PaymentAggregate.create(
                1001L, 2001L, new BigDecimal("99.99"), 1, "attach_data"
            );

            assertThat(event.getPaymentId()).isNotNull();
            assertThat(event.getPaymentNo()).startsWith("PAY");
            assertThat(event.getOrderId()).isEqualTo(1001L);
            assertThat(event.getUserId()).isEqualTo(2001L);
            assertThat(event.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(event.getPaymentMethod()).isEqualTo(1);
        }

        @Test
        @DisplayName("orderId 为空抛出异常")
        void create_withNullOrderId_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                null, 2001L, new BigDecimal("99.99"), 1, null
            )).isInstanceOf(BusinessException.class)
              .hasMessageContaining("订单ID不能为空");
        }

        @Test
        @DisplayName("userId 为空抛出异常")
        void create_withNullUserId_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                1001L, null, new BigDecimal("99.99"), 1, null
            )).isInstanceOf(BusinessException.class)
              .hasMessageContaining("用户ID不能为空");
        }

        @Test
        @DisplayName("amount 为空抛出异常")
        void create_withNullAmount_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                1001L, 2001L, null, 1, null
            )).isInstanceOf(BusinessException.class)
              .hasMessageContaining("支付金额不能为空");
        }

        @Test
        @DisplayName("amount 小于等于零抛出异常")
        void create_withZeroAmount_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                1001L, 2001L, BigDecimal.ZERO, 1, null
            )).isInstanceOf(BusinessException.class)
              .hasMessageContaining("支付金额必须大于0");
        }

        @Test
        @DisplayName("paymentMethod 为空抛出异常")
        void create_withNullPaymentMethod_throws() {
            assertThatThrownBy(() -> PaymentAggregate.create(
                1001L, 2001L, new BigDecimal("99.99"), null, null
            )).isInstanceOf(BusinessException.class)
              .hasMessageContaining("支付方式不能为空");
        }
    }

    @Nested
    @DisplayName("pay 支付方法")
    class PayTests {

        @Test
        @DisplayName("待支付状态支付成功")
        void pay_withPendingStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING.getCode());

            PaymentSucceededEvent event = (PaymentSucceededEvent) aggregate.pay(mockStrategy);

            assertThat(event).isNotNull();
            assertThat(event.getTransactionId()).isNotNull();
            assertThat(aggregate.getStatus()).isEqualTo(PaymentStatus.SUCCESS.getCode());
            assertThat(aggregate.getTransactionId()).isNotNull();
        }

        @Test
        @DisplayName("非待支付状态支付抛出异常")
        void pay_withNonPendingStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS.getCode());

            assertThatThrownBy(() -> aggregate.pay(mockStrategy))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("refund 退款方法")
    class RefundTests {

        @Test
        @DisplayName("已支付状态全额退款成功")
        void refund_withSuccessStatus_fullRefund() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS.getCode());
            aggregate.setAmount(new BigDecimal("100.00"));

            var event = aggregate.refund(new BigDecimal("100.00"), mockStrategy);

            assertThat(event).isNotNull();
            assertThat(aggregate.getStatus()).isEqualTo(PaymentStatus.REFUNDED.getCode());
            assertThat(aggregate.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("已支付状态部分退款成功")
        void refund_withSuccessStatus_partialRefund() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS.getCode());
            aggregate.setAmount(new BigDecimal("100.00"));
            aggregate.setRefundedAmount(BigDecimal.ZERO);

            var event = aggregate.refund(new BigDecimal("30.00"), mockStrategy);

            assertThat(event).isNotNull();
            assertThat(aggregate.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED.getCode());
            assertThat(aggregate.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("退款金额超过支付金额抛出异常")
        void refund_withAmountExceed_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS.getCode());
            aggregate.setAmount(new BigDecimal("100.00"));

            assertThatThrownBy(() -> aggregate.refund(new BigDecimal("150.00"), mockStrategy))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("退款金额不能超过支付金额");
        }

        @Test
        @DisplayName("累计退款金额超过支付金额抛出异常")
        void refund_withTotalExceed_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS.getCode());
            aggregate.setAmount(new BigDecimal("100.00"));
            aggregate.setRefundedAmount(new BigDecimal("80.00"));

            assertThatThrownBy(() -> aggregate.refund(new BigDecimal("30.00"), mockStrategy))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("累计退款金额不能超过支付金额");
        }

        @Test
        @DisplayName("待支付状态不能退款")
        void refund_withPendingStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING.getCode());

            assertThatThrownBy(() -> aggregate.refund(new BigDecimal("100.00"), mockStrategy))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("fail 失败方法")
    class FailTests {

        @Test
        @DisplayName("待支付状态标记失败成功")
        void fail_withPendingStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING.getCode());

            PaymentFailedEvent event = aggregate.fail("支付超时");

            assertThat(event).isNotNull();
            assertThat(event.getReason()).isEqualTo("支付超时");
            assertThat(aggregate.getStatus()).isEqualTo(PaymentStatus.FAILED.getCode());
        }

        @Test
        @DisplayName("非待支付状态标记失败抛出异常")
        void fail_withNonPendingStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS.getCode());

            assertThatThrownBy(() -> aggregate.fail("支付超时"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有待支付状态可以标记为失败");
        }
    }

    @Nested
    @DisplayName("close 关闭方法")
    class CloseTests {

        @Test
        @DisplayName("待支付状态关闭成功")
        void close_withPendingStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.PENDING.getCode());

            aggregate.close();

            assertThat(aggregate.getStatus()).isEqualTo(PaymentStatus.CLOSED.getCode());
        }

        @Test
        @DisplayName("支付失败状态关闭成功")
        void close_withFailedStatus_success() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.FAILED.getCode());

            aggregate.close();

            assertThat(aggregate.getStatus()).isEqualTo(PaymentStatus.CLOSED.getCode());
        }

        @Test
        @DisplayName("已支付状态关闭抛出异常")
        void close_withSuccessStatus_throws() {
            PaymentAggregate aggregate = createTestAggregate(PaymentStatus.SUCCESS.getCode());

            assertThatThrownBy(() -> aggregate.close())
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("fromEntity 和 toEntity 转换方法")
    class EntityConversionTests {

        @Test
        @DisplayName("fromEntity 正确转换")
        void fromEntity_convertsCorrectly() {
            Payment payment = Payment.builder()
                .id(1001L)
                .paymentNo("PAY123456")
                .orderId(2001L)
                .userId(3001L)
                .amount(new BigDecimal("99.99"))
                .refundedAmount(BigDecimal.ZERO)
                .paymentMethod(1)
                .status(PaymentStatus.SUCCESS.getCode())
                .transactionId("TXN123")
                .refundReason("已退款")
                .refundTime(LocalDateTime.now())
                .attach("attach")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

            PaymentAggregate aggregate = PaymentAggregate.fromEntity(payment);

            assertThat(aggregate.getId()).isEqualTo(1001L);
            assertThat(aggregate.getPaymentNo()).isEqualTo("PAY123456");
            assertThat(aggregate.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(aggregate.getStatus()).isEqualTo(PaymentStatus.SUCCESS.getCode());
        }

        @Test
        @DisplayName("fromEntity 返回 null 当输入为 null")
        void fromEntity_withNull_returnsNull() {
            assertThat(PaymentAggregate.fromEntity(null)).isNull();
        }

        @Test
        @DisplayName("toEntity 正确转换")
        void toEntity_convertsCorrectly() {
            PaymentAggregate aggregate = PaymentAggregate.builder()
                .id(1001L)
                .paymentNo("PAY123456")
                .orderId(2001L)
                .userId(3001L)
                .amount(new BigDecimal("99.99"))
                .refundedAmount(BigDecimal.ZERO)
                .paymentMethod(1)
                .status(PaymentStatus.SUCCESS.getCode())
                .transactionId("TXN123")
                .build();

            Payment payment = aggregate.toEntity();

            assertThat(payment.getId()).isEqualTo(1001L);
            assertThat(payment.getPaymentNo()).isEqualTo("PAY123456");
            assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS.getCode());
        }
    }

    private PaymentAggregate createTestAggregate(Integer status) {
        return PaymentAggregate.builder()
            .id(System.currentTimeMillis())
            .paymentNo("PAY" + System.currentTimeMillis())
            .orderId(1001L)
            .userId(2001L)
            .amount(new BigDecimal("100.00"))
            .refundedAmount(BigDecimal.ZERO)
            .paymentMethod(1)
            .status(status)
            .build();
    }
}
