package com.cartethyia.easyorange.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * 限流规则 yaml 契约 — 守住「键名拼错静默退化成不限方法」这个坑。
 *
 * <p>{@code Rule} 的集合字段叫 {@code methods}，Spring 宽松绑定只做大小写 / 连字符归一，
 * <b>不做单复数推断</b>：写 {@code method:} 不报错、启动不失败，值被静默丢弃，空列表在
 * {@code RateLimitFilter#matchesMethod} 里等于「匹配所有方法」—— 叠加「首条命中即返回」，
 * 商品浏览的 GET 会吃本地 200/min 而非写规则的 Redis 30/min，反向同理。
 * 单元测试直接 {@code new Rule(...)} 绕过 yaml，故只有本测试能拦住。
 */
@DisplayName("限流规则 yaml 契约")
class RateLimitRulesContractTest {

    private static final List<String> CONFIG_FILES =
            List.of("application.yaml", "application-dev.yaml", "application-prod.yaml");

    @Test
    @DisplayName("三份 yaml 的每条限流规则都写 methods（单数 method 不绑定，会退化成匹配所有方法）")
    void everyRuleDeclaresMethods() {
        for (String file : CONFIG_FILES) {
            for (Map<?, ?> rule : rulesOf(file)) {
                String where = "%s 的规则 %s".formatted(file, rule.get("path-pattern"));
                assertThat(rule.containsKey("method"))
                        .as(
                                "%s 用了单数 method 键 —— Spring 宽松绑定只做大小写/连字符归一，不做单复数推断，"
                                        + "该键被静默忽略后空 methods 等于「匹配所有 HTTP 方法」。改回 methods",
                                where)
                        .isFalse();
                assertThat(blankMethods(rule.get("methods")))
                        .as("%s 的 methods 缺失或为空 —— 空 = 匹配所有 HTTP 方法，会与相邻写规则的口径打架", where)
                        .isFalse();
            }
        }
    }

    /** 标量（{@code methods: GET}）与列表（{@code methods: [GET, POST]}）都合法，空值两种形态都算违规。 */
    private static boolean blankMethods(Object declared) {
        if (declared instanceof List<?> list) {
            return list.isEmpty();
        }
        return declared == null || String.valueOf(declared).isBlank();
    }

    /** 汇总一份 yaml 全部文档里 rate-limit-filter.rules 的条目（application.yaml 是多文档，profile 段各带一份）。 */
    private static List<Map<?, ?>> rulesOf(String file) {
        List<Map<?, ?>> rules = new ArrayList<>();
        for (Object document : documentsOf(file)) {
            Object filter = dig(document, "rate-limit-filter");
            Object declared = dig(filter, "rules");
            if (declared instanceof List<?> list) {
                for (Object rule : list) {
                    if (rule instanceof Map<?, ?> map) {
                        rules.add(map);
                    }
                }
            }
        }
        return rules;
    }

    private static List<Object> documentsOf(String file) {
        List<Object> documents = new ArrayList<>();
        try (InputStream yaml =
                RateLimitRulesContractTest.class.getClassLoader().getResourceAsStream(file)) {
            assertThat(yaml).as("测试资源缺失：%s", file).isNotNull();
            // loadAll 惰性解析，必须在流关闭前物化
            new Yaml().loadAll(yaml).forEach(documents::add);
        } catch (Exception e) {
            throw new IllegalStateException("解析 " + file + " 失败", e);
        }
        return documents;
    }

    /** 逐层取嵌套 map 的键值，缺失即 null（让断言报「哪里缺了」，而不是一个无信息的栈）。 */
    private static Object dig(Object root, String key) {
        return root instanceof Map<?, ?> map ? map.get(key) : null;
    }
}
