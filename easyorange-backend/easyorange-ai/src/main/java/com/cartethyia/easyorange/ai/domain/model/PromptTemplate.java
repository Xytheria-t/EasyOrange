package com.cartethyia.easyorange.ai.domain.model;

/** Prompt 模板 — 版本化的 Prompt 资源。template 正文即 system prompt 原文、不做占位符渲染（业务变量由服务侧 String.format 填进 user 消息）；同名多版本由 getLatest 取最大版本号。 */
public record PromptTemplate(String name, String version, String template, String description) {}
