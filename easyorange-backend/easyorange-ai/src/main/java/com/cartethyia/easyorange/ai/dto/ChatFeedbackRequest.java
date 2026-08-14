package com.cartethyia.easyorange.ai.dto;

/**
 * AI 输出反馈（👍/👎）— 反馈飞轮入口，导出后自动扩充金标准评测集。
 */
public record ChatFeedbackRequest(
        String scope, String question, String answer, boolean helpful, String comment, String callLogId) {}
