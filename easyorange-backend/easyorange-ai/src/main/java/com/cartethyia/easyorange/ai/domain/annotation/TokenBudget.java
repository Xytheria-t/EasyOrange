package com.cartethyia.easyorange.ai.domain.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Token 预算注解 — 标注在 AI 调用方法上，声明该场景的 token 预算。
 * <p>
 * 由 {@link TokenBudgetAspect} 拦截，调用前检查是否超预算。
 *
 * @param scenario          预算场景名（{@link com.cartethyia.easyorange.ai.domain.enums.AiCallScope#budgetScenario()}
 *     的取值，如 "chat" / "auto_listing" / "semantic"）。注解只能写字面量、编译期发现不了它与枚举脱节，
 *     契约由 {@code TokenBudgetScenarioContractTest} 反射钉死
 * @param maxTokensPerCall  单次调用最大 token 数
 * @param dailyTokenLimit   每日总预算（0 = 不限）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TokenBudget {

    String scenario();

    int maxTokensPerCall();

    int dailyTokenLimit() default 0;
}
