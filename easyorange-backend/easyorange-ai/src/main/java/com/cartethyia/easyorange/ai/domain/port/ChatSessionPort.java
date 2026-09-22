package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import java.util.List;

/**
 * 多轮对话短期记忆端口 — 按 sessionId 存取最近若干轮对话；轮数窗口由实现侧配置单点决定、调用方不指定
 * （读写共用一个旋钮，避免「写入裁 N 轮、读取取 M 轮」两处漂移）。
 * <p>
 * 写入以「一轮对话」为单位（{@code user + assistant} 一次提交）：一轮的两次写入不可分割，
 * 存储侧一次落盘，不会出现只存了提问没存回答的半轮记忆。
 * <p>
 * 实现方在 adapter/outbound（Redis List 会话窗口）；后端不可用或会话为空时返回空列表
 * （fail-open：丢记忆不阻塞回答）。
 */
public interface ChatSessionPort {

    void saveTurns(String sessionId, List<ChatTurn> turns);

    List<ChatTurn> loadRecent(String sessionId);
}
