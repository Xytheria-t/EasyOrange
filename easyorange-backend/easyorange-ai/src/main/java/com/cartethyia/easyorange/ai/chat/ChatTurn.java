package com.cartethyia.easyorange.ai.chat;

/**
 * 对话轮次 — 短期记忆的最小单元（Redis 会话窗口内按序存取）。
 */
public record ChatTurn(String role, String content) {}
