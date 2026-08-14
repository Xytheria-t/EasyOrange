package com.cartethyia.easyorange.ai.chat;

/**
 * 工具决策器输出（单步 ReAct）— 模型决定是否需要检索知识库，以及是否从对话提取用户偏好。
 *
 * @param tool       knowledge_search / none
 * @param query      改写后的检索关键词
 * @param preference 提取到的用户偏好（无则 null）
 */
public record ToolDecision(String tool, String query, Preference preference) {

    public record Preference(String key, String value) {}
}
