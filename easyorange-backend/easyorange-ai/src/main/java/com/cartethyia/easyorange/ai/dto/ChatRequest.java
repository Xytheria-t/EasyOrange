package com.cartethyia.easyorange.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * AI 对话请求 — 多轮会话靠 sessionId 关联（短期记忆在 Redis，TTL 24h）。
 *
 * @param question    用户问题
 * @param sessionId   会话 ID（前端生成，首轮可空）
 * @param forceFresh  跳过语义缓存（评估/回归用，线上请求保持 false）
 */
public record ChatRequest(
        @NotBlank(message = "问题不能为空") String question, String sessionId, boolean forceFresh) {}
