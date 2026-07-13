package com.cartethyia.easyorange.ai.prompt;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt 模板渲染器 — 将 {@code {var}} 占位符替换为实际变量值。
 * <p>
 * 不引入模板引擎，用 {@link Matcher#replaceAll(String)} 实现。
 * 缺失变量抛出 {@link IllegalArgumentException}。
 * 使用 {@link Matcher#quoteReplacement(String)} 防止 {@code $} 和 {@code \} 被特殊解释。
 */
public final class PromptRenderer {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)}");

    private PromptRenderer() {
    }

    /**
     * 渲染模板，将所有 {@code {var}} 占位符替换为变量值。
     *
     * @param template 模板
     * @param variables 变量映射
     * @return 渲染后的字符串
     * @throws IllegalArgumentException 如果模板中有未提供的变量
     */
    public static String render(PromptTemplate template, Map<String, Object> variables) {
        var matcher = VARIABLE_PATTERN.matcher(template.template());
        var result = new StringBuilder();
        while (matcher.find()) {
            var key = matcher.group(1);
            if (!variables.containsKey(key)) {
                throw new IllegalArgumentException("缺少模板变量: " + key);
            }
            var replacement = String.valueOf(variables.get(key));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
