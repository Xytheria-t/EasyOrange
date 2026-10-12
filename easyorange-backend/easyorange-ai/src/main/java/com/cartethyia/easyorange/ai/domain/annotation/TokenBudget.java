package com.cartethyia.easyorange.ai.domain.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Token 预算注解 — 标注在 AI 调用方法上，由 {@link
 * com.cartethyia.easyorange.ai.adapter.outbound.budget.TokenBudgetAspect} 调用前拦截检查是否超预算。
 * <p>
 * {@code scenario} 预算场景名（{@link com.cartethyia.easyorange.ai.domain.enums.AiCallScope#budgetScenario()}
 * 的取值，如 "chat" / "auto_listing" / "semantic"）；{@code dailyTokenLimit} 每日总预算（0 = 不限）。
 * 注解只能写字面量、编译期发现不了它与枚举脱节，契约由 {@code TokenBudgetScenarioContractTest} 反射钉死。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TokenBudget {

    String scenario();

    int maxTokensPerCall();

    int dailyTokenLimit() default 0;
}
