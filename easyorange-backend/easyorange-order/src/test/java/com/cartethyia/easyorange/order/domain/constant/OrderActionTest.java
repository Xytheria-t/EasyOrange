package com.cartethyia.easyorange.order.domain.constant;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("OrderAction 状态机单元测试")
class OrderActionTest {

    static Stream<Arguments> actionSourceCases() {
        return Stream.of(
                Arguments.of(OrderAction.PAY, Set.of(OrderStatus.PENDING_PAYMENT)),
                Arguments.of(OrderAction.CANCEL, Set.of(OrderStatus.PENDING_PAYMENT)),
                Arguments.of(OrderAction.FORCE_CANCEL, Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID)),
                Arguments.of(OrderAction.SHIP, Set.of(OrderStatus.PAID)),
                Arguments.of(OrderAction.CONFIRM_RECEIPT, Set.of(OrderStatus.SHIPPED)),
                Arguments.of(OrderAction.REFUND, Set.of(OrderStatus.PAID, OrderStatus.SHIPPED)));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("actionSourceCases")
    @DisplayName("canApply 仅允许声明的前置状态")
    void canApply_onlyAllowsDeclaredSources(OrderAction action, Set<OrderStatus> expectedSources) {
        for (OrderStatus status : OrderStatus.values()) {
            boolean expected = expectedSources.contains(status);
            assertThat(action.canApply(status, PaymentStatus.PAID))
                    .as("%s from %s", action, status)
                    .isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("REFUND 要求支付状态为已支付")
    void canApply_refundRequiresPaidPayment() {
        assertThat(OrderAction.REFUND.canApply(OrderStatus.PAID, PaymentStatus.UNPAID))
                .isFalse();
        assertThat(OrderAction.REFUND.canApply(OrderStatus.PAID, PaymentStatus.PAID))
                .isTrue();
        assertThat(OrderAction.REFUND.canApply(OrderStatus.SHIPPED, PaymentStatus.PAID))
                .isTrue();
        assertThat(OrderAction.REFUND.canApply(OrderStatus.SHIPPED, PaymentStatus.UNPAID))
                .isFalse();
    }

    @Test
    @DisplayName("非退款动作不受支付状态约束")
    void canApply_nonRefundActionsIgnorePaymentStatus() {
        assertThat(OrderAction.PAY.canApply(OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID))
                .isTrue();
        assertThat(OrderAction.CANCEL.canApply(OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID))
                .isTrue();
        assertThat(OrderAction.FORCE_CANCEL.canApply(OrderStatus.PAID, PaymentStatus.PAID))
                .isTrue();
        assertThat(OrderAction.SHIP.canApply(OrderStatus.PAID, PaymentStatus.PAID))
                .isTrue();
        assertThat(OrderAction.CONFIRM_RECEIPT.canApply(OrderStatus.SHIPPED, PaymentStatus.PAID))
                .isTrue();
    }

    @Test
    @DisplayName("终端状态无任何动作可触发")
    void canApply_terminalStatesHaveNoActions() {
        for (OrderStatus terminal :
                new OrderStatus[] {OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.REFUNDED}) {
            for (OrderAction action : OrderAction.values()) {
                assertThat(action.canApply(terminal, PaymentStatus.PAID))
                        .as("%s from terminal %s", action, terminal)
                        .isFalse();
            }
        }
    }

    @Test
    @DisplayName("动作目标状态与支付副作用声明正确")
    void actionMetadata_targetsAndPaymentEffects() {
        assertThat(OrderAction.PAY.target()).isEqualTo(OrderStatus.PAID);
        assertThat(OrderAction.PAY.targetPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(OrderAction.CANCEL.target()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(OrderAction.FORCE_CANCEL.target()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(OrderAction.SHIP.target()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(OrderAction.CONFIRM_RECEIPT.target()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(OrderAction.REFUND.target()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(OrderAction.REFUND.targetPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("关闭类动作按归因类型记录原因")
    void actionMetadata_closureKindForCloseActions() {
        assertThat(OrderAction.CANCEL.closureKind()).isEqualTo(ClosureKind.CANCEL);
        assertThat(OrderAction.FORCE_CANCEL.closureKind()).isEqualTo(ClosureKind.CANCEL);
        assertThat(OrderAction.REFUND.closureKind()).isEqualTo(ClosureKind.REFUND);
        assertThat(OrderAction.PAY.closureKind()).isEqualTo(ClosureKind.NONE);
        assertThat(OrderAction.SHIP.closureKind()).isEqualTo(ClosureKind.NONE);
        assertThat(OrderAction.CONFIRM_RECEIPT.closureKind()).isEqualTo(ClosureKind.NONE);
    }

    @Test
    @DisplayName("非前置状态触发动作应返回对应错误码")
    void actionMetadata_resultCodePerAction() {
        assertThat(OrderAction.PAY.resultCode()).isEqualTo(OrderResultCode.ORDER_STATUS_ERROR);
        assertThat(OrderAction.CANCEL.resultCode()).isEqualTo(OrderResultCode.ORDER_CANNOT_CANCEL);
        assertThat(OrderAction.FORCE_CANCEL.resultCode()).isEqualTo(OrderResultCode.ORDER_STATUS_ERROR);
        assertThat(OrderAction.SHIP.resultCode()).isEqualTo(OrderResultCode.ORDER_STATUS_ERROR);
        assertThat(OrderAction.CONFIRM_RECEIPT.resultCode()).isEqualTo(OrderResultCode.ORDER_STATUS_ERROR);
        assertThat(OrderAction.REFUND.resultCode()).isEqualTo(OrderResultCode.ORDER_CANNOT_REFUND);
    }
}
