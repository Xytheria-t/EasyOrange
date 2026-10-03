package com.cartethyia.easyorange.ai.application.support;

import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;

/**
 * 循环的决策函数 — 拿当轮消息序列与工具 schema，返回模型要执行的工具调用。
 * <p>
 * 决策在各链路里绑定的模型路由与记账口径不同（chat 走 chat 场景快模型、listing 的决策调用记进
 * auto_listing 场景预算），所以内核收函数不收实现：重试一次、解析失败整轮作废这些协议层口径由各
 * 决策实现自持，内核只认「空列表 = 本轮决策失败」。
 */
public interface ToolLoopDecider {

    /** 返回空列表即本轮决策失败，由内核按 Spec 注入的降级动作收口。 */
    List<ToolLoopCall> decide(String sessionId, List<Message> decisionMessages, List<ToolCallback> toolCallbacks);
}
