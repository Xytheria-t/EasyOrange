package com.cartethyia.easyorange.ai.domain.model;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户偏好 — 长期记忆条目（从对话中提取，跨会话持久）。
 */
public record UserPreference(String key, String value) {

    /**
     * 画像块的文本渲染（每行 {@code key: value}）—— 决策上下文（{@code AgentLoopRunner}）与生成
     * prompt（{@code ChatPromptAssembler}）两处装配共用：同一份画像在两处渲染成同一种形状，空画像的
     * 缺省标记也就只有一处定义。
     */
    public static String format(List<UserPreference> preferences) {
        return preferences.isEmpty()
                ? "(无)"
                : preferences.stream()
                        .map(preference -> preference.key() + ": " + preference.value())
                        .collect(Collectors.joining("\n"));
    }
}
