package com.cartethyia.easyorange.adapter.inbound.web.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 知识库文档新增请求 — 新增即摄入（分块 → embed → ES）。
 *
 * @param title   文档标题
 * @param content 文档正文（Markdown / 纯文本）
 * @param source  来源标注（缺省由前端填「运营」）
 */
public record CreateKnowledgeDocRequest(
        @NotBlank(message = "文档标题不能为空") String title,
        @NotBlank(message = "文档正文不能为空") String content,
        String source) {}
