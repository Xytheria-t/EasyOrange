package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.support.AiModelRouter;
import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.application.support.FailureReason;
import com.cartethyia.easyorange.ai.application.support.ToolLoopCall;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import java.util.ArrayList;
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
 * 决策器 — 一轮里「调哪个工具、参数是什么」的发起方，工具 schema 随请求下发，模型以原生 tool calling 返回决策。
 * 模型调用 / 路由选型 / 参数反序列化收在一处：它们在编排器里只服务于这一个决策动作，摊开会让「循环控制」
 * 与「一次模型调用的技术细节」共用一张依赖清单。
 * <p>
 * 降级口径：调用故障 / 参数 JSON 不可解析 → 同模型重试一次，仍失败返回空列表交编排器单步降级（快模型输出
 * 不稳定，一次抖动丢掉整个多步能力比多付一次决策调用更贵）；「未返回工具调用」是协议层确定性响应，不重试。
 * 同轮有一个调用解析不了就整轮作废：半执行一轮会让回填的 assistant tool_calls 与 role=tool 观察对不上，
 * 协议不合法，因此不做「跳过坏调用、继续执行其余」。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ToolCallDecider {

    private final AiModelSupport aiModelSupport;
    private final AiModelRouter modelRouter;
    private final ObjectMapper objectMapper;

    /**
     * 取一轮的工具调用决策；空列表即「本轮决策失败」，调用方负责降级。
     * 一轮可以带回多个调用，全部返回 —— 供应商侧的并行发起（规则 + 找货同问最常见）在这里省掉一整轮往返。
     */
    List<ToolCallDecision> decide(
            @Nullable String sessionId, List<Message> decisionMessages, List<ToolCallback> toolCallbacks) {
        return decideInternal(sessionId, decisionMessages, toolCallbacks, AiCallScope.CHAT);
    }

    /**
     * 编排内核的决策入口 — 输出 {@link ToolLoopCall} 视图。模型路由场景固定 {@code chat_tool}（决策是纯
     * 路由任务，chat / listing 两条链路共用快模型），记账口径由 {@code scope} 指定（listing 的决策调用要
     * 记进 auto_listing 场景预算）；{@code toolInputOf} 按各链路工具面取入参摘要，供 trace 展示。
     */
    public List<ToolLoopCall> decideForLoop(
            @Nullable String sessionId,
            List<Message> decisionMessages,
            List<ToolCallback> toolCallbacks,
            AiCallScope scope,
            Function<ToolCallDecision, String> toolInputOf) {
        return decideInternal(sessionId, decisionMessages, toolCallbacks, scope).stream()
                .map(decision -> new ToolLoopCall(
                        decision.tool(),
                        decision.parsedArguments().thought(),
                        toolInputOf.apply(decision),
                        decision.rawArguments(),
                        decision.rawToolCall()))
                .toList();
    }

    private List<ToolCallDecision> decideInternal(
            @Nullable String sessionId,
            List<Message> decisionMessages,
            List<ToolCallback> toolCallbacks,
            AiCallScope scope) {
        try {
            return decideOnce(sessionId, decisionMessages, toolCallbacks, scope);
        } catch (Exception first) {
            // 瞬时抖动（超时 / 坏 JSON）的自愈窗口：重试一次，仍失败才交编排器单步降级
            log.warn("action=tool_call_decision_retry, sessionId={}, reason={}", sessionId, FailureReason.of(first));
        }
        try {
            return decideOnce(sessionId, decisionMessages, toolCallbacks, scope);
        } catch (Exception second) {
            log.warn(
                    "action=tool_call_decision_failed, fallback=single_step, sessionId={}, reason={}",
                    sessionId,
                    FailureReason.of(second));
            return List.of();
        }
    }

    private List<ToolCallDecision> decideOnce(
            @Nullable String sessionId,
            List<Message> decisionMessages,
            List<ToolCallback> toolCallbacks,
            AiCallScope scope) {
        List<AssistantMessage.ToolCall> toolCalls = aiModelSupport.callWithTools(
                modelRouter.choose(AiModelRouter.SCENARIO_CHAT_TOOL), scope, decisionMessages, toolCallbacks);
        if (toolCalls.isEmpty()) {
            log.warn(
                    "action=tool_call_decision_failed, fallback=single_step, sessionId={}, reason=模型未返回工具调用",
                    sessionId);
            return List.of();
        }
        var decisions = new ArrayList<ToolCallDecision>(toolCalls.size());
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            decisions.add(ToolCallDecision.of(parseArgs(toolCall), toolCall));
        }
        if (decisions.size() > 1) {
            log.info("action=tool_call_parallel, sessionId={}, count={}", sessionId, decisions.size());
        }
        return List.copyOf(decisions);
    }

    private ToolCallArguments parseArgs(AssistantMessage.ToolCall toolCall) {
        return objectMapper.readValue(toolCall.arguments(), ToolCallArguments.class);
    }
}
