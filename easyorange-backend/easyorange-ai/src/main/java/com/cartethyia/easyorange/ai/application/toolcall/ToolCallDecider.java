package com.cartethyia.easyorange.ai.application.toolcall;

import com.cartethyia.easyorange.ai.application.support.FailureReason;
import com.cartethyia.easyorange.ai.application.support.ModelCaller;
import com.cartethyia.easyorange.ai.application.support.ModelRouter;
import com.cartethyia.easyorange.ai.domain.enums.ModelCallScope;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 工具决策器 —— 一轮里「调哪个工具、参数是什么」的发起方：把当前全部消息与工具面交给模型，取回它选的原生
 * {@code tool_calls} 并反序列化成参数。
 * <p>
 * 在链路里的位置：{@link RoundDecider} 的实现（各链路 {@code roundDecider()}）把它绑上记账场景与入参摘要后交内核
 * —— 内核每轮调一次，只认「这一轮要执行哪些调用」，不认背后是不是模型。模型调用 / 路由选型 / 参数反序列化都收在这里，
 * 免得「循环控制」与「一次模型调用的技术细节」共用依赖清单。
 * <p>
 * 降级口径：调用故障 / 参数解析失败 → 同模型重试一次，仍失败返回空列表交编排器单步降级（快模型输出不稳，一次
 * 抖动丢掉整条多步链比多付一次决策调用更贵）；模型未返回工具调用是协议层确定性响应，不重试。同轮有一个调用解析
 * 不了整轮作废，不做「跳过坏调用、继续执行其余」——半执行会让回填的 tool_calls 与观察对不上，协议不合法。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ToolCallDecider {

    private final ModelCaller modelCaller;
    private final ObjectMapper objectMapper;

    /**
     * 编排内核的决策入口 — 输出 {@link ToolLoopCall} 视图。路由角色恒为 {@code decision}（决策是纯路由任务，
     * chat 与 listing 两条链路共用快模型），故写死；记账 {@code scope} 与入参摘要 {@code toolInputOf} 随链路注入
     * ——模型选型两条链路相同、记账不同，一个写死一个传参不是疏漏。一轮可带回多个调用并全部返回——模型的并行
     * 发起（规则 + 找货同问最常见）在这里省掉一整轮往返。
     */
    public List<ToolLoopCall> decide(
            @Nullable String sessionId,
            List<Message> messages,
            List<ToolCallback> callbacks,
            ModelCallScope scope,
            Function<ToolCallDecision, String> toolInputOf) {
        return decideWithRetry(sessionId, messages, callbacks, scope).stream()
                .map(decision -> new ToolLoopCall(
                        decision.tool(),
                        decision.parsedArguments().thought(),
                        toolInputOf.apply(decision),
                        decision.rawArguments(),
                        decision.rawToolCall()))
                .toList();
    }

    private List<ToolCallDecision> decideWithRetry(
            @Nullable String sessionId, List<Message> messages, List<ToolCallback> callbacks, ModelCallScope scope) {
        try {
            return decideOnce(sessionId, messages, callbacks, scope);
        } catch (Exception first) {
            log.warn("action=tool_call_decision_retry, sessionId={}, reason={}", sessionId, FailureReason.of(first));
        }
        try {
            return decideOnce(sessionId, messages, callbacks, scope);
        } catch (Exception second) {
            log.warn(
                    "action=tool_call_decision_failed, fallback=single_step, sessionId={}, reason={}",
                    sessionId,
                    FailureReason.of(second));
            return List.of();
        }
    }

    private List<ToolCallDecision> decideOnce(
            @Nullable String sessionId, List<Message> messages, List<ToolCallback> callbacks, ModelCallScope scope) {
        List<AssistantMessage.ToolCall> toolCalls =
                modelCaller.callWithTools(ModelRouter.SCENARIO_DECISION, scope, messages, callbacks);
        if (toolCalls.isEmpty()) {
            log.warn(
                    "action=tool_call_decision_failed, fallback=single_step, sessionId={}, reason=模型未返回工具调用",
                    sessionId);
            return List.of();
        }
        List<ToolCallDecision> decisions = toolCalls.stream()
                .map(toolCall -> ToolCallDecision.of(
                        objectMapper.readValue(toolCall.arguments(), ToolCallArguments.class), toolCall))
                .toList();
        if (decisions.size() > 1) {
            log.info("action=tool_call_parallel, sessionId={}, count={}", sessionId, decisions.size());
        }
        return decisions;
    }
}
