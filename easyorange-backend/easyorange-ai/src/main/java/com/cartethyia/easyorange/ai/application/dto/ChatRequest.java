package com.cartethyia.easyorange.ai.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI 对话请求 — 多轮会话靠 sessionId 关联（短期记忆在 Redis，TTL 24h）。
 *
 * @param question    用户问题；长度上限 2000 字是成本护栏 —— question 原样进 stale 缓存键、embedding
 *                    调用与生成 prompt（token 裁剪只裁历史、不裁当前问题），限流管频次、预算管总量，
 *                    长度管单次。2000 字按 TokenEstimator 的 CJK 0.7 口径 ≈ 1400 token，贴 chat 场景
 *                    maxTokensPerCall=1500 的单次预算
 * @param sessionId   会话 ID（前端生成，首轮可空）
 * @param forceFresh  跳过语义缓存（评估/回归用，线上请求保持 false）
 */
public record ChatRequest(
        @NotBlank(message = "问题不能为空") @Size(max = 2000, message = "问题长度不能超过 2000 字") String question,
        String sessionId,
        boolean forceFresh) {}
