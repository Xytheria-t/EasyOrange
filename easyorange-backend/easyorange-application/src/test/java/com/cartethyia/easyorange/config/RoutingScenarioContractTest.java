package com.cartethyia.easyorange.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.application.support.AiModelRouter;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * 模型路由场景契约 — 守住「代码引用的场景键必须在 yaml 配置」这条静默失效线。
 *
 * <p>{@code AiModelRouter.choose} 对未配置的场景回退 default-model：<b>键缺失不报错</b>，症状是「图片
 * 分析悄悄走了文本模型」这类降级而非异常。场景名在 {@link AiModelRouter} 单点定义（消费方引用常量，
 * 不写字面量），本测试钉住 application.yaml 与这批常量的对应关系——加了新场景忘配 yaml，这里红。
 */
@DisplayName("模型路由场景契约")
class RoutingScenarioContractTest {

    @Test
    @DisplayName("代码引用的全部场景键都在 application.yaml 配置且 bean 名非空")
    void yamlCoversAllReferencedScenarios() throws Exception {
        Map<?, ?> scenarios = routingScenarios();

        for (String scenario :
                Set.of(AiModelRouter.SCENARIO_CHAT_TOOL, AiModelRouter.SCENARIO_VISION, AiModelRouter.SCENARIO_JUDGE)) {
            Object beanName = scenarios.get(scenario);
            assertThat(beanName)
                    .as("easyorange.ai.routing.scenarios.%s 未配置 —— 该场景会静默回退 default-model", scenario)
                    .isNotNull();
            assertThat(String.valueOf(beanName))
                    .as("easyorange.ai.routing.scenarios.%s 的 bean 名为空", scenario)
                    .isNotBlank();
        }
    }

    @Test
    @DisplayName("default-model 已显式配置（场景键缺失时的回退目标不能靠 Spring 占位符兜）")
    void defaultModelConfigured() throws Exception {
        assertThat(routing("default-model"))
                .as("easyorange.ai.routing.default-model 未配置")
                .isNotNull();
    }

    @SuppressWarnings("unchecked")
    private static Map<?, ?> routingScenarios() throws Exception {
        Object scenarios = routing("scenarios");
        assertThat(scenarios).as("easyorange.ai.routing.scenarios 段缺失").isInstanceOf(Map.class);
        return (Map<?, ?>) scenarios;
    }

    private static Object routing(String key) throws Exception {
        try (InputStream yaml =
                RoutingScenarioContractTest.class.getClassLoader().getResourceAsStream("application.yaml")) {
            assertThat(yaml).as("测试资源缺失：application.yaml").isNotNull();
            // application.yaml 是多文档（--- 分隔 profile），easyorange.ai 段落在首个文档里
            for (Object document : new Yaml().loadAll(yaml)) {
                if (document == null) {
                    continue;
                }
                Object routing = dig(document, "easyorange", "ai", "routing");
                if (routing != null) {
                    return ((Map<?, ?>) routing).get(key);
                }
            }
            throw new AssertionError("application.yaml 未找到 easyorange.ai.routing 段");
        }
    }

    /** 逐层取嵌套 map 的键值，缺失即 null（让断言报「哪里缺了」而不是 NPE）。 */
    private static Object dig(Object root, String... keys) {
        Object current = root;
        for (String key : keys) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(key);
        }
        return current;
    }
}
