package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import java.util.List;

/**
 * 多轮对话短期记忆端口 — 按「用户 + 会话」存取最近若干轮；轮数窗口由实现侧配置单点决定（读写共用
 * 一个旋钮，避免「写入裁 N 轮、读取取 M 轮」漂移）。实现在 adapter/outbound（Redis List），后端不可用
 * 或会话为空返回空列表（fail-open：丢记忆不阻塞回答）。
 * <p>
 * 键绑定 userId 即归属校验：sessionId 由前端生成、可伪造，只按它存取等于把他人对话记忆交给持有 id 的人
 * （水平越权）；写入以一轮为单位一次落盘，不留半轮。24h 非持久缓存，键格式变更不迁移、旧键自然过期。
 */
public interface ChatSessionPort {

    void saveTurns(String userId, String sessionId, List<ChatTurn> turns);

    List<ChatTurn> loadRecent(String userId, String sessionId);
}
