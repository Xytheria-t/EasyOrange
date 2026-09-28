package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.support.AiModelRouter;
import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 决策器 — 一轮里「调哪个工具、参数是什么」的发起方，工具 schema 随请求下发，模型以原生 tool calling 返回决策。
 * <p>
 * 取舍：把这三件事（模型调用 / 路由选型 / 参数反序列化）收在一处，是因为它们在编排器里只服务于这一个决策动作；
 * 摊在编排器构造器上会让「循环控制」与「一次模型调用的技术细节」共用一张依赖清单。
 * <p>
 * 降级口径：调用故障 / 未返回工具调用 / 任一调用参数 JSON 不可解析，一律返回空列表由编排器走单步降级 ——
 * 循环内不重试，一次请求最多一次决策故障。同轮有一个调用解析不了就整轮作废：半执行一轮会让回填的
 * assistant tool_calls 与 role=tool 观察对不上，协议不合法。因此这里不做「跳过坏调用、继续执行其余」。
 */
@Slf4j
@Component
public class ToolCallDecider {

    private final AiModelSupport aiModelSupport;
    private final AiModelRouter modelRouter;
    private final ObjectMapper objectMapper;

    ToolCallDecider(AiModelSupport aiModelSupport, AiModelRouter modelRouter, ObjectMapper objectMapper) {
        this.aiModelSupport = aiModelSupport;
        this.modelRouter = modelRouter;
        this.objectMapper = objectMapper;
    }

    /**
     * 取一轮的工具调用决策；空列表即「本轮决策失败」，调用方负责降级。
     * 一轮可以带回多个调用，全部返回 —— 供应商侧的并行发起（规则 + 找货同问最常见）在这里省掉一整轮往返。
     */
    List<ToolCallDecision> decide(
            @Nullable String sessionId, List<Message> decisionMessages, List<ToolCallback> toolCallbacks) {
        try {
            List<AssistantMessage.ToolCall> toolCalls = aiModelSupport.callWithTools(
                    modelRouter.choose("chat_tool"), AiCallScope.CHAT, decisionMessages, toolCallbacks);
            if (toolCalls.isEmpty()) {
                log.warn(
                        "action=agent_decision_failed, fallback=single_step, sessionId={}, reason=模型未返回工具调用",
                        sessionId);
                return List.of();
            }
            var decisions = new ArrayList<ToolCallDecision>(toolCalls.size());
            for (AssistantMessage.ToolCall toolCall : toolCalls) {
                decisions.add(ToolCallDecision.of(parseArgs(toolCall), toolCall));
            }
            if (decisions.size() > 1) {
                log.info("action=agent_parallel_tool_calls, sessionId={}, count={}", sessionId, decisions.size());
            }
            return List.copyOf(decisions);
        } catch (Exception e) {
            log.warn(
                    "action=agent_decision_failed, fallback=single_step, sessionId={}, reason={}",
                    sessionId,
                    failureReason(e));
            return List.of();
        }
    }

    private ToolCallArguments parseArgs(AssistantMessage.ToolCall toolCall) {
        String arguments = toolCall.arguments();
        return objectMapper.readValue(arguments == null ? "" : arguments, ToolCallArguments.class);
    }

    private static String failureReason(Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
