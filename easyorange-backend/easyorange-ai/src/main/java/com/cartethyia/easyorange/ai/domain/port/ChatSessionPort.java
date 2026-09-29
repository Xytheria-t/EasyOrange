package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import java.util.List;

/**
 * 多轮对话短期记忆端口 — 按「用户 + 会话」存取最近若干轮对话；轮数窗口由实现侧配置单点决定
 * （读写共用一个旋钮，避免「写入裁 N 轮、读取取 M 轮」两处漂移）。
 * <p>
 * 键绑定 userId 即归属校验：sessionId 由前端生成并随请求回传、可伪造，只按 sessionId 存取等于把他人对话
 * 记忆交给持有 id 的人读写（水平越权）。写入以「一轮对话」为单位一次落盘，不留半轮记忆。
 * <p>
 * 实现方在 adapter/outbound（Redis List）；后端不可用或会话为空时返回空列表（fail-open：丢记忆不阻塞回答）。
 * 24h 过期的非持久缓存，键格式变更不做迁移、旧键自然过期。
 */
public interface ChatSessionPort {

    void saveTurns(String userId, String sessionId, List<ChatTurn> turns);

    List<ChatTurn> loadRecent(String userId, String sessionId);
}
