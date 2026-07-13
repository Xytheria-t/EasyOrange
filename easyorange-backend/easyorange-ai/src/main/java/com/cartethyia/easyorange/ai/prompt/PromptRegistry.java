package com.cartethyia.easyorange.ai.prompt;

import java.util.List;
import java.util.Optional;

/**
 * Prompt 模板注册中心 — 按 name + version 查找版本化的 Prompt 模板。
 */
public interface PromptRegistry {

    /**
     * 按 name + version 精确查找模板。
     *
     * @param name    模板名
     * @param version 版本号
     * @return 模板，不存在时返回 {@link Optional#empty()}
     */
    Optional<PromptTemplate> get(String name, String version);

    /**
     * 获取指定模板名的最新版本（按语义化版本排序）。
     *
     * @param name 模板名
     * @return 最新版本模板，不存在时返回 {@link Optional#empty()}
     */
    Optional<PromptTemplate> getLatest(String name);

    /**
     * 列出指定模板名的所有版本。
     *
     * @param name 模板名
     * @return 所有版本的模板列表，不存在时返回空列表
     */
    List<PromptTemplate> listVersions(String name);
}
