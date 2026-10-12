package com.cartethyia.easyorange.ai.domain.port;

import org.jspecify.annotations.Nullable;

/**
 * AI 输出反馈写入端口 — 用户对一次找货回答点「有用 / 没用」，是金标准集的原料（导出见
 * {@link GoldenSetExportPort}）；实现在 adapter/outbound（{@code eo_ai_feedback} 表），
 * 记录失败只告警不抛出：反馈是观测副产物，点一次「有用」不该让请求失败。
 */
public interface FeedbackPort {

    void record(
            String scope,
            String question,
            String answer,
            boolean helpful,
            @Nullable String comment,
            @Nullable String callLogId,
            @Nullable String userId);
}
