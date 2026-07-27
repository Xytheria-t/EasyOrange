package com.cartethyia.easyorange.ai.prompt;

import java.util.List;
import java.util.Optional;

/**
 * 测试用 PromptRegistry 桩 — 返回固定的 stub 模板，避免依赖 classpath YAML 文件。
 * <p>
 * 服务层单元测试只关心业务逻辑（JSON 解析、异常处理、空值兜底），
 * 不验证 prompt 内容本身（由 {@code PromptContentTest} 单独覆盖）。
 */
public final class TestPromptRegistry implements PromptRegistry {

    public static final PromptTemplate STUB = new PromptTemplate(
            "stub", "v1.0.0", "stub system prompt", "test stub");

    @Override
    public Optional<PromptTemplate> get(String name, String version) {
        return Optional.of(new PromptTemplate(name, version, "stub system prompt", "test stub"));
    }

    @Override
    public Optional<PromptTemplate> getLatest(String name) {
        return Optional.of(new PromptTemplate(name, "v1.0.0", "stub system prompt", "test stub"));
    }

    @Override
    public List<PromptTemplate> listVersions(String name) {
        return List.of();
    }
}
