package com.cartethyia.easyorange.payment.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.payment.application.command.PayCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCallbackCommand;
import com.cartethyia.easyorange.payment.application.command.PaymentCommandHandler;
import com.cartethyia.easyorange.payment.application.command.PaymentPhaseExecutor;
import com.cartethyia.easyorange.payment.application.command.RefundPaymentCommand;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事务边界守卫 — 防止两阶段事务静默失效的回归。
 * <p>
 * phase 方法若被合并回 {@link PaymentCommandHandler} 成为同类自调用，Spring 代理会被绕过、
 * {@code @Transactional} 静默失效，聚合更新与 Outbox 事件发布将退化为两次独立提交
 * （崩溃窗口内支付成功但事件丢失）。纯 Mockito 单测无 Spring 代理，无法发现该回归，
 * 故以反射校验结构不变量：
 * <ol>
 *   <li>全部 phase 方法声明在独立 Bean {@link PaymentPhaseExecutor} 且标注 {@link Transactional}；</li>
 *   <li>编排方法 {@code pay(PayCommand)} / {@code refundPayment(RefundPaymentCommand)} /
 *       {@code processCallback(PaymentCallbackCommand)} 与订单侧入口
 *       {@code payByOrderId} / {@code refundByOrderId} 不得持有事务（事务不得跨网关调用，见 ADR-0007）。</li>
 * </ol>
 */
@DisplayName("支付事务边界守卫")
class PaymentTransactionBoundaryTest {

    private static final Set<String> TRANSACTIONAL_PHASE_METHODS = Set.of(
            "preparePayPhase1",
            "confirmPayPhase2",
            "rollbackPayStatus",
            "prepareRefundPhase1",
            "confirmRefundPhase2",
            "rollbackRefundStatus");

    /** 编排方法名 → 首参命令类型；改名或改签名都会让下方事务扫描漏检，故由存在性断言兜住。 */
    private static final Map<String, String> ORCHESTRATION_METHODS = Map.of(
            "pay", PayCommand.class.getName(),
            "refundPayment", RefundPaymentCommand.class.getName(),
            "processCallback", PaymentCallbackCommand.class.getName());

    /** 订单侧入口（以 orderId 为键）— 同样编排两阶段，不得持有事务。 */
    private static final Set<String> ORDER_ID_ENTRY_POINTS = Set.of("payByOrderId", "refundByOrderId");

    @Test
    void phaseMethodsAreDeclaredOnExecutorAndTransactional() {
        for (String name : TRANSACTIONAL_PHASE_METHODS) {
            assertThat(handlerMethod(name))
                    .as("phase 方法 %s 不得声明在 PaymentCommandHandler（同类自调用会绕过事务代理）", name)
                    .isNull();

            Method phaseMethod = executorMethod(name);
            assertThat(phaseMethod)
                    .as("phase 方法 %s 必须声明在 PaymentPhaseExecutor", name)
                    .isNotNull();
            assertThat(phaseMethod.isAnnotationPresent(Transactional.class))
                    .as("phase 方法 %s 必须标注 @Transactional", name)
                    .isTrue();
        }
    }

    @Test
    void orchestrationMethodsDoNotHoldTransactions() {
        for (Method method : PaymentCommandHandler.class.getDeclaredMethods()) {
            String expectedCommand = ORCHESTRATION_METHODS.get(method.getName());
            boolean isOrchestration = (expectedCommand != null
                            && method.getParameterCount() == 1
                            && method.getParameterTypes()[0].getName().equals(expectedCommand))
                    || ORDER_ID_ENTRY_POINTS.contains(method.getName());
            if (isOrchestration) {
                assertThat(method.isAnnotationPresent(Transactional.class))
                        .as("编排方法 %s 不得持有事务（事务不得跨网关调用）", method)
                        .isFalse();
            }
        }
    }

    /**
     * 编排方法必须存在且首参命令类型未变 — 否则上面的扫描会因改名/改签名而静默失效，门禁形同虚设。
     */
    @Test
    void orchestrationMethodsArePresentWithExpectedCommand() {
        ORCHESTRATION_METHODS.forEach((name, commandType) -> {
            Method method = handlerMethod(name);
            assertThat(method).as("编排方法 %s 必须声明在 PaymentCommandHandler", name).isNotNull();
            assertThat(method.getParameterTypes()[0].getName())
                    .as("编排方法 %s 的首参必须是 %s", name, commandType)
                    .isEqualTo(commandType);
        });
    }

    /**
     * 订单侧入口必须存在 — 否则上面的扫描会因改名而静默失效，门禁形同虚设。
     */
    @Test
    void orderIdEntryPointsArePresent() {
        for (String name : ORDER_ID_ENTRY_POINTS) {
            assertThat(handlerMethod(name))
                    .as("订单侧入口 %s 必须声明在 PaymentCommandHandler", name)
                    .isNotNull();
        }
    }

    private static Method handlerMethod(String name) {
        return Arrays.stream(PaymentCommandHandler.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private static Method executorMethod(String name) {
        return Arrays.stream(PaymentPhaseExecutor.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
