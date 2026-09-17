package com.cartethyia.easyorange.ai.domain.model;

/**
 * Prompt 模板 — 版本化的 Prompt 资源。
 * <p>
 * 模板正文直接当 system prompt 使用（原 {@code PromptRenderer} 的 {@code {var}} 占位符渲染已移除），
 * 业务变量由服务侧内联 {@code String.format} 填进 user 消息。
 *
 * @param name        模板名（如 "ai_pricing_system"）
 * @param version     语义化版本（如 "v1.0.0"），同名多版本时 {@code getLatest} 取最大者
 * @param template    模板正文（system prompt）
 * @param description 模板描述
 */
public record PromptTemplate(String name, String version, String template, String description) {}
