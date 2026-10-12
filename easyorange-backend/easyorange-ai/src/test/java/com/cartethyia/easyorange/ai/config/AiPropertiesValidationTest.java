package com.cartethyia.easyorange.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.testsupport.PropertyBindings;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 配置约束的绑定期契约 —— {@code @Validated} 与嵌套 {@code @Valid} 级联只在起服时生效，挂漏一处
 * {@code @Valid} 的表现是约束静默失效（非法值照样绑定成功，运行时才出怪值）。本测试用同一套
 * jakarta Validator 把「哪个槽位受校验、0 是不是非法值」钉在不起服也能跑的形式上。
 */
@DisplayName("AiProperties 约束契约")
class AiPropertiesValidationTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("嵌套槽位的约束经 @Valid 级联 —— 非法值在绑定期被拦")
    void nestedConstraintsAreEnforced() {
        assertViolates("chat.max-rounds", "0", "chat.maxRounds");
        assertViolates("listing.max-rounds", "11", "listing.maxRounds");
        assertViolates("semantic-cache.similarity-threshold", "1.5", "semanticCache.similarityThreshold");
        assertViolates("embedding.dimensions", "0", "embedding.dimensions");
    }

    @Test
    @DisplayName("预算字段的 0 是「关」不是「耗尽」——不触发校验")
    void zeroBudgetMeansOffNotExhausted() {
        var props = PropertyBindings.bind(
                AiProperties.class,
                "budget.scenarios.chat.max-tokens-per-call",
                "0",
                "budget.scenarios.chat.daily-token-limit",
                "0");
        assertThat(VALIDATOR.validate(props)).isEmpty();
    }

    @Test
    @DisplayName("两条默认值路径（裸绑 / 构造器兜底）产出的都是合法实例")
    void bothFallbackPathsProduceValidInstances() {
        assertThat(VALIDATOR.validate(PropertyBindings.bind(AiProperties.class)))
                .isEmpty();
        assertThat(VALIDATOR.validate(
                        new AiProperties(null, null, null, null, null, null, null, null, null, null, null)))
                .isEmpty();
    }

    private static void assertViolates(String key, String value, String path) {
        assertThat(VALIDATOR.validate(PropertyBindings.bind(AiProperties.class, key, value)))
                .as("%s=%s 未产生校验违规 —— @Valid 是否挂漏？", key, value)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly(path);
    }
}
