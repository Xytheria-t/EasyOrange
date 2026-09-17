package com.cartethyia.easyorange.ai.testsupport;

import com.cartethyia.easyorange.ai.domain.model.PromptTemplate;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import java.util.Optional;

/**
 * 测试用 PromptRegistry 桩 — 默认返回以模板名拼成的 stub 正文，避免依赖 classpath YAML 文件。
 * <p>
 * 正文里带上模板名，是为了让「多个 LLM 调用打同一个 mock」的测试能用
 * {@code withSystemContaining("search_intent_system")} 区分调用来自哪个模板；
 * 模板内容本身由 {@code PromptContentTest} 单独覆盖。
 * <p>
 * 验「模板缺失」路径用 {@link #empty()}，不要在每个测试里再手写一遍匿名实现。
 */
public final class TestPromptRegistry implements PromptRegistry {

    @Override
    public Optional<PromptTemplate> getLatest(String name) {
        return Optional.of(new PromptTemplate(name, "v1.0.0", "stub system prompt: " + name, "test stub"));
    }

    /** 任何模板都查不到 —— 用于验证调用方在模板缺失时的行为。 */
    public static PromptRegistry empty() {
        return name -> Optional.empty();
    }
}
