package com.cartethyia.easyorange.ai.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * AI 输出反馈（赞/踩）— 反馈飞轮入口，仅 helpful=1 的 chat 反馈可导出进金标准评测集（口径见 GoldenSetExportService）。
 */
public record ChatFeedbackRequest(
        @NotBlank(message = "反馈场景不能为空") String scope,
        @NotBlank(message = "问题不能为空") String question,
        @NotBlank(message = "回答不能为空") String answer,
        boolean helpful,
        String comment,
        String callLogId) {}
