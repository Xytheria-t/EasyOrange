package com.cartethyia.easyorange.ai.testsupport;

import com.cartethyia.easyorange.ai.domain.model.PromptTemplate;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistryPort;
import java.util.Optional;

/**
 * 测试用 PromptRegistryPort 桩 — 返回以模板名拼成的 stub 正文（不依赖 classpath YAML）；正文带模板名，
 * 是为了让多个 LLM 调用打同一 mock 时能用 {@code withSystemContaining("auto_listing")} 区分来源；
 * 模板内容本身由 {@code PromptContentTest} 覆盖。
 * <p>
 * 验「模板缺失」路径用 {@link #empty()}，别在每个测试里手写匿名实现。
 */
public final class TestPromptRegistry implements PromptRegistryPort {

    @Override
    public Optional<PromptTemplate> getLatest(String name) {
        return Optional.of(new PromptTemplate(name, "v1.0.0", "stub system prompt: " + name, "test stub"));
    }

    /** 任何模板都查不到 —— 用于验证调用方在模板缺失时的行为。 */
    public static PromptRegistryPort empty() {
        return name -> Optional.empty();
    }
}
