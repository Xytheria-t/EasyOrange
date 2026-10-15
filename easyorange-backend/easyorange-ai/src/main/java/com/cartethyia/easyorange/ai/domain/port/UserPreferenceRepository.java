package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.UserPreference;
import java.util.List;

/**
 * 用户长期偏好仓储端口 — 对话长期记忆：从对话提取的偏好写入 eo_user_preference，
 * 聊天时注入 prompt（用户偏好表 / 向量记忆库的演进位是「按相关性召回」，当前量级直接全量注入）。
 * <p>
 * 在链路里的位置：实现在 adapter/outbound 的 {@code UserPreferenceRepositoryImpl}；写方是 {@code ChatTools}
 * 的 remember_preference 工具（唯一写路径），读方是 {@code ChatAppService} 的偏好块装配。
 */
public interface UserPreferenceRepository {

    List<UserPreference> findByUserId(String userId);

    /** upsert：同一 (userId, prefKey) 存在则更新值，否则插入。 */
    void record(String userId, String key, String value);
}
