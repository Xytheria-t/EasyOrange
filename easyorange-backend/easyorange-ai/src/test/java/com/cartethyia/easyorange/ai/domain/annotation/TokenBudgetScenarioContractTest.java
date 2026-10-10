package com.cartethyia.easyorange.ai.domain.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.adapter.outbound.QueryEmbeddingAdapter;
import com.cartethyia.easyorange.ai.application.chat.AiChatAppService;
import com.cartethyia.easyorange.ai.application.listing.AutoListingAppService;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalAppService;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@code @TokenBudget(scenario = ...)} 与 {@link AiCallScope#budgetScenario()} 的契约测试。
 * <p>
 * 注解 {@code scenario} 只能写字面量，与枚举脱节编译期发现不了、后果还静默：切面按注解场景查用量、
 * 记账按枚举场景写入，预算永远显示「没花超」。所以带注解的类必须逐个反射钉死 —— 场景名改了或
 * 漏登记新注解，这里直接红。
 */
@DisplayName("@TokenBudget 场景契约（注解字面量 == AiCallScope 场景键）-> 测试")
class TokenBudgetScenarioContractTest {

    /** 全部带 {@code @TokenBudget} 的类 —— 新增带预算的调用点必须登记进来，否则本测试失去覆盖面。 */
    private static final List<Class<?>> BUDGETED_CLASSES = List.of(
            AiChatAppService.class,
            AutoListingAppService.class,
            QueryEmbeddingAdapter.class,
            KnowledgeRetrievalAppService.class);

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

    /**
     * 登记了却没挂注解要让测试变红 —— 上条只校验「存在的注解写对了没有」，摘掉注解它只会少跑几个
     * 参数化用例，预算闸门静默消失。
     */
    @Test
    @DisplayName("登记进清单的类必须有方法带 @TokenBudget（摘掉注解不能让用例静静变少）")
    void everyRegisteredClassCarriesAnnotation() {
        assertThat(BUDGETED_CLASSES)
                .allSatisfy(type -> assertThat(Arrays.stream(type.getDeclaredMethods())
                                .anyMatch(method -> method.isAnnotationPresent(TokenBudget.class)))
                        .as("%s 已登记为预算调用点，但没有任何方法带 @TokenBudget", type.getSimpleName())
                        .isTrue());
    }
}
