package com.cartethyia.easyorange.ai.domain.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.adapter.outbound.QueryEmbeddingAdapter;
import com.cartethyia.easyorange.ai.application.chat.AiChatAppService;
import com.cartethyia.easyorange.ai.application.listing.AutoListingAppService;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@code @TokenBudget(scenario = ...)} 与 {@link AiCallScope#budgetScenario()} 的契约测试。
 * <p>
 * 注解的 {@code scenario} 只能写字面量，编译期发现不了它与枚举脱节；而脱节的后果是静默的 ——
 * 切面按注解里的场景查用量、记账按枚举场景写入，两边各记各的，预算永远显示「没花超」，
 * 到账单上才发现。所以三处注解逐个反射钉死：场景名改了、忘了同步枚举或漏了一处新注解，这里直接红。
 */
@DisplayName("@TokenBudget 场景契约（注解字面量 == AiCallScope 场景键）-> 测试")
class TokenBudgetScenarioContractTest {

    /** 全部带 {@code @TokenBudget} 的类 —— 新增带预算的调用点必须登记进来，否则本测试失去覆盖面。 */
    private static final List<Class<?>> BUDGETED_CLASSES =
            List.of(AiChatAppService.class, AutoListingAppService.class, QueryEmbeddingAdapter.class);

    static List<Arguments> budgetedMethods() {
        return BUDGETED_CLASSES.stream()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(TokenBudget.class))
                .map(method -> Arguments.of(
                        method.getDeclaringClass().getSimpleName() + "#" + method.getName(),
                        method.getAnnotation(TokenBudget.class).scenario()))
                .toList();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("budgetedMethods")
    @DisplayName("注解 scenario 必须是某个 AiCallScope 的场景键（否则记账与检查落进两个场景）")
    void scenarioMatchesAiCallScope(String method, String scenario) {
        assertThat(AiCallScope.values())
                .as("%s 的 scenario='%s' 不在 AiCallScope 的场景键取值域内", method, scenario)
                .extracting(AiCallScope::budgetScenario)
                .contains(scenario);
    }
}
