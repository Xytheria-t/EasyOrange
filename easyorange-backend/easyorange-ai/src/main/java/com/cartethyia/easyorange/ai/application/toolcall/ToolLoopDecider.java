package com.cartethyia.easyorange.ai.application.toolcall;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;

/**
 * 循环的决策函数 — 内核收函数不收实现：模型路由、记账场景与重试等协议口径随链路（chat / listing）
 * 不同，由各决策实现自持。
 */
@FunctionalInterface
public interface ToolLoopDecider {

    /** 返回空列表即本轮决策失败，由内核按 Spec 注入的降级动作收口。 */
    List<ToolLoopCall> decide(
            @Nullable String sessionId, List<Message> decisionMessages, List<ToolCallback> toolCallbacks);
}
