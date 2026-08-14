package com.cartethyia.easyorange.ai.chat;

/**
 * 用户偏好 — 长期记忆条目（从对话中提取，跨会话持久）。
 */
public record UserPreference(String key, String value) {}
