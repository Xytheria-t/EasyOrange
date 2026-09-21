package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import java.util.List;

/**
 * 多轮对话短期记忆端口 — 按 sessionId 存取最近若干轮对话。
 * <p>
 * 写入以「一轮对话」为单位（{@code user + assistant} 一次提交）：一轮的两次写入不可分割，
 * 存储侧能一次落盘，也不会出现只存了提问没存回答的半轮记忆。
 * <p>
 * 实现方在 adapter/outbound（Redis List 会话窗口）；后端不可用或会话为空时返回空列表
 * （fail-open：丢记忆不阻塞回答）。
 */
public interface ChatSessionPort {

    void saveTurns(String sessionId, List<ChatTurn> turns);

    List<ChatTurn> loadRecent(String sessionId, int limit);
}
