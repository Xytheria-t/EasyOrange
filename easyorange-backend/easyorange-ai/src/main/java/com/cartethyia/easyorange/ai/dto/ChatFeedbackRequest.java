package com.cartethyia.easyorange.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * AI 输出反馈（👍/👎）— 反馈飞轮入口，导出后自动扩充金标准评测集。
 */
public record ChatFeedbackRequest(
        @NotBlank(message = "反馈场景不能为空") String scope,
        @NotBlank(message = "问题不能为空") String question,
        @NotBlank(message = "回答不能为空") String answer,
        boolean helpful,
        String comment,
        String callLogId) {}
