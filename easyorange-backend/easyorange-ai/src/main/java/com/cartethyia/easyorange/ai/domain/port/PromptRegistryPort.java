package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.PromptTemplate;
import java.util.Optional;

/** Prompt 模板注册中心 — 按 name + version 查找版本化的 Prompt 模板。
 * <p>
 * 在链路里的位置：实现在 adapter/outbound 的 {@code YamlPromptRegistry}（读 {@code resources/prompts/*.yml}）；
 * 消费方是两条链路的编排器（取 system prompt）与 {@code Judge}（取评审量表）。 */
public interface PromptRegistryPort {

    /** 取最新版模板（按语义化版本排序）；不存在返回 {@link Optional#empty()}。 */
    Optional<PromptTemplate> getLatest(String name);

    /**
     * 取模板正文，缺失时 fail-fast —— 模板名写错是部署期配置错误，静默用空 prompt 跑模型会把
     * 「配置错」伪装成「模型答得差」。
     */
    default String require(String name) {
        return getLatest(name)
                .map(PromptTemplate::template)
                .orElseThrow(() -> new IllegalStateException("Prompt template not found: " + name));
    }
}
