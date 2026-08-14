package com.cartethyia.easyorange.ai.chat;

import java.util.List;

/**
 * 用户长期画像仓储端口 — Agent 长期记忆：从对话提取的偏好写入 eo_user_preference，
 * 聊天时注入 prompt（用户画像表 / 向量记忆库的演进位是「按相关性召回」，当前量级直接全量注入）。
 */
public interface UserPreferenceRepository {

    List<UserPreference> findByUserId(String userId);

    /** upsert：同一 (userId, prefKey) 存在则更新值，否则插入。 */
    void record(String userId, String key, String value);
}
