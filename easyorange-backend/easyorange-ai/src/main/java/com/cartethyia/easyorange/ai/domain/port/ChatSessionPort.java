package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import java.util.List;

/**
 * 多轮对话短期记忆端口 — 按 sessionId 存取最近若干轮对话。
 * <p>
 * 实现方在 adapter/outbound（Redis List 会话窗口）；后端不可用或会话为空时返回空列表
 * （fail-open：丢记忆不阻塞回答）。
 */
public interface ChatSessionPort {

    void saveTurn(String sessionId, String role, String content);

    List<ChatTurn> loadRecent(String sessionId, int limit);
}
