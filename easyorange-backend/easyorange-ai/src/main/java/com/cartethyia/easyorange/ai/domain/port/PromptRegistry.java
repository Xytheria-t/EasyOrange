package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.PromptTemplate;
import java.util.Optional;

/**
 * Prompt 模板注册中心 — 按 name + version 查找版本化的 Prompt 模板。
 */
public interface PromptRegistry {

    /**
     * 获取指定模板名的最新版本（按语义化版本排序）。
     *
     * @param name 模板名
     * @return 最新版本模板，不存在时返回 {@link Optional#empty()}
     */
    Optional<PromptTemplate> getLatest(String name);

    /**
     * 取模板正文，模板缺失时 fail-fast。
     * <p>
     * 调用方一律走本方法：模板名写错是部署期配置错误，静默用空 prompt 跑模型会
     * 把「配置错」伪装成「模型答得差」，还不如启动/调用时直接炸出来。
     */
    default String require(String name) {
        return getLatest(name)
                .map(PromptTemplate::template)
                .orElseThrow(() -> new IllegalStateException("Prompt template not found: " + name));
    }
}
