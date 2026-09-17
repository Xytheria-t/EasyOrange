package com.cartethyia.easyorange.ai.application.dto;

import java.util.List;

/**
 * 审核建议 — {@code suggestedAction} 直接驱动管理端「采纳 AI 建议」按钮，
 * AI 不可用时必须给 false + {@code AI_UNAVAILABLE} 标记（见 {@code AiReviewService.unavailable}）。
 */
public record AiReviewResult(
        boolean suggestedAction,
        String suggestedActionDesc,
        int confidenceScore,
        List<String> riskFlags,
        String reasoning) {}
