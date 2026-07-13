package com.cartethyia.easyorange.ai.budget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Token 预算注解 — 标注在 AI 调用方法上，声明该场景的 token 预算。
 * <p>
 * 由 {@link TokenBudgetAspect} 拦截，调用前检查是否超预算。
 *
 * @param scenario          场景名（如 "product_tag_generation"）
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
