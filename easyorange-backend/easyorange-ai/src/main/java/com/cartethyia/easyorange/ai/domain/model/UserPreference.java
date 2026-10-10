package com.cartethyia.easyorange.ai.domain.model;

import java.util.List;
import java.util.stream.Collectors;

/** 用户偏好 — 长期记忆条目（从对话提取，跨会话持久）。 */
public record UserPreference(String key, String value) {

    /**
     * 偏好块文本渲染 — 决策上下文（{@code ToolCallLoop}）与生成 prompt（{@code ChatPromptAssembler}）
     * 两处装配共用，空偏好缺省标记只有一处定义。
     */
    public static String format(List<UserPreference> preferences) {
        return preferences.isEmpty()
                ? "(无)"
                : preferences.stream()
                        .map(preference -> preference.key() + ": " + preference.value())
                        .collect(Collectors.joining("\n"));
    }
}
