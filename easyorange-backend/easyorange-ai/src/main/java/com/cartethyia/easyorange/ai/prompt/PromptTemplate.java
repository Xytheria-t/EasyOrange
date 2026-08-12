package com.cartethyia.easyorange.ai.prompt;

/**
 * Prompt 模板 — 版本化的 Prompt 资源。
 * <p>
 * 模板内容使用 {@code {var}} 占位符。
 *
 * @param name        模板名（如 "product_tag_generation"）
 * @param version     语义化版本（如 "v1.0.0"）
 * @param template    模板内容（含 {var} 占位符）
 * @param description 模板描述
 */
public record PromptTemplate(String name, String version, String template, String description) {}
