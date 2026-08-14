package com.cartethyia.easyorange.ai.dto;

import java.util.List;

/**
 * AI 对话回答 — 带引用溯源（来源标题，回答末尾用 [来源:标题] 标注）。
 */
public record ChatAnswer(String answer, List<String> sources, String sessionId) {}
