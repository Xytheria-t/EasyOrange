package com.cartethyia.easyorange.ai.application.toolcall;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;

/**
 * 每轮的决策函数 — 内核收函数不收实现：模型路由、记账场景与重试等协议口径随链路（chat / listing）不同，
 * 由各决策实现自持；返回空列表即本轮决策失败，由内核按 Spec 注入的降级动作收口。
 * <p>
 * 与 {@link ToolCallDecider}（调模型的决策器）分属两层：链路把它绑定场景与入参摘要后填进
 * {@link ToolLoopKernel.Spec}，内核只认「每轮一批调用」，不认背后是否走了模型。
 */
@FunctionalInterface
public interface RoundDecider {

    List<ToolCallStep> decide(@Nullable String sessionId, List<Message> messages, List<ToolCallback> callbacks);
}
