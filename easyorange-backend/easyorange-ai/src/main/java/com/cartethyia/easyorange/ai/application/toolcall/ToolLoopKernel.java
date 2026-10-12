package com.cartethyia.easyorange.ai.application.toolcall;

import com.cartethyia.easyorange.ai.application.support.FailureReason;
import com.cartethyia.easyorange.ai.domain.model.ToolCallStepTrace;
import com.cartethyia.easyorange.ai.domain.model.ToolCallStepView;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.ai.domain.port.ToolCallStepTracePort;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * 多步工具调用循环（ReAct）的编排内核 — 逐轮「决策 → 工具 → 观察」推进到模型判定信息足够。
 * <p>
 * 手写循环 + 原生 tool calling：{@code ChatModel.call} 不自动执行工具，步数与降级控制权留在本类。内核只持
 * 链路无关的机制（四条出口、协议回填、步级 trace/SSE、toolPath），首轮上下文、模型路由与降级动作由
 * {@link Spec} / {@link ToolLoopDecider} 注入 —— chat 与 listing 共用，不改写各自的工具语义；trace/step/
 * 指标失败不碰主链路。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ToolLoopKernel {

    /** 收敛轮工具名 — 两条工具面共享的协议词汇；各链路常量与之同字面，由测试钉住。 */
    private static final String FINISH_TOOL = "finish";

    private final ToolCallStepTracePort tracePort;
    private final IdGenerator idGenerator;

    /**
     * 一次循环的规格 — 链路差异全在这里注入；{@code userId} 是已归属的值（评估跑批传 null），归属判定
     * 留在各链路编排器（同一口径还管工具面的偏好归属）。
     */
    public record Spec(
            String systemPrompt,
            String firstUserMessage,
            List<ToolCallback> toolCallbacks,
            int maxSteps,
            BooleanSupplier budgetExhausted,
            /* 决策失败出口的降级动作（chat 按原始问题补检索一次、listing 无操作），异常自担不外抛。 */
            Runnable decisionFailedFallback,
            @Nullable String sessionId,
            @Nullable String userId,
            @Nullable ChatStreamHandler streamHandler,
            ToolLoopListener listener) {}

    /** 循环结果 — outcome / rounds 供指标与降级归因；toolPath 含 finish 轮，供路由准确率评估对照金标准集。 */
    public record Outcome(ToolCallLoopOutcome outcome, int rounds, List<String> toolPath) {}

    public Outcome run(Spec spec, ToolLoopDecider decider) {
        try {
            Outcome outcome = executeLoop(spec, decider);
            spec.listener().recordLoop(outcome.outcome(), outcome.rounds());
            return outcome;
        } catch (RuntimeException e) {
            spec.listener().recordLoopFailure();
            throw e;
        }
    }

    private Outcome executeLoop(Spec spec, ToolLoopDecider decider) {
        String traceId = idGenerator.generateId();
        var dispatcher = ToolDispatcher.of(spec.toolCallbacks());
        var messages = new DecisionMessages(spec.systemPrompt(), spec.firstUserMessage());
        var toolPath = new ArrayList<String>();
        int rounds = 0;
        int nextStepIndex = 1;

        for (int round = 1; round <= spec.maxSteps(); round++) {
            if (round > 1 && spec.budgetExhausted().getAsBoolean()) {
                log.warn(
                        "action=tool_call_loop_degraded, reason=budget, sessionId={}, rounds={}",
                        spec.sessionId(),
                        rounds);
                return new Outcome(ToolCallLoopOutcome.BUDGET, rounds, List.copyOf(toolPath));
            }
            List<ToolLoopCall> calls = decider.decide(spec.sessionId(), messages.snapshot(), spec.toolCallbacks());
            if (calls.isEmpty()) {
                spec.decisionFailedFallback().run();
                return new Outcome(ToolCallLoopOutcome.DECISION_FAILED, rounds, List.copyOf(toolPath));
            }
            rounds = round;
            RoundResult roundResult = executeCalls(spec, traceId, dispatcher, messages, calls, nextStepIndex);
            nextStepIndex = roundResult.nextStepIndex();
            toolPath.addAll(roundResult.toolPath());
            if (roundResult.finished()) {
                return new Outcome(ToolCallLoopOutcome.FINISHED, rounds, List.copyOf(toolPath));
            }
        }
        log.warn(
                "action=tool_call_loop_degraded, reason=step_limit, sessionId={}, rounds={}, toolPath={}",
                spec.sessionId(),
                rounds,
                String.join(",", toolPath));
        return new Outcome(ToolCallLoopOutcome.STEP_LIMIT, rounds, List.copyOf(toolPath));
    }

    /**
     * 执行一轮的全部调用并落成观测副产物（trace / SSE step / 步级指标），再按对话协议回填；步序跨轮连续，
     * 否则一轮内多个工具挤进同一 stepIndex，trace 里两个动作看起来是同一步。
     */
    private RoundResult executeCalls(
            Spec spec,
            String traceId,
            ToolDispatcher dispatcher,
            DecisionMessages messages,
            List<ToolLoopCall> calls,
            int firstStepIndex) {
        // finish 先摘出去：执行体里就没有「跳过它」的分支，回填的 tool_calls 与观察天然等长；多个 finish 取最后一个
        ToolLoopCall finish = null;
        var executableCalls = new ArrayList<ToolLoopCall>(calls.size());
        for (ToolLoopCall call : calls) {
            if (FINISH_TOOL.equals(call.tool())) {
                finish = call;
            } else {
                executableCalls.add(call);
            }
        }

        var toolPath = new ArrayList<String>(executableCalls.size() + 1);
        var observations = new ArrayList<String>(executableCalls.size());
        int stepIndex = firstStepIndex;
        for (ToolLoopCall call : executableCalls) {
            DispatchResult result = executeOneTool(spec, traceId, stepIndex++, dispatcher, call);
            toolPath.add(call.tool());
            observations.add(result.observation());
        }
        if (finish != null) {
            recordFinishStep(spec, traceId, stepIndex, finish);
            toolPath.add(finish.tool());
            return new RoundResult(stepIndex + 1, toolPath, true);
        }
        messages.appendRound(rawToolCallsOf(executableCalls), observations);
        return new RoundResult(stepIndex, toolPath, false);
    }

    private DispatchResult executeOneTool(
            Spec spec, String traceId, int stepIndex, ToolDispatcher dispatcher, ToolLoopCall call) {
        long start = System.nanoTime();
        DispatchResult result = dispatcher.dispatch(call);
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        spec.listener().recordTool(call.tool(), latencyMs);
        recordToolStep(spec, traceId, stepIndex, call, result, latencyMs);
        return result;
    }

    /** 按模型给出的顺序取原始 tool call —— 回填时 assistant 与 role=tool 两侧须同序。 */
    private static List<AssistantMessage.ToolCall> rawToolCallsOf(List<ToolLoopCall> calls) {
        return calls.stream().map(ToolLoopCall::rawToolCall).toList();
    }

    /** 落一步工具步 trace 并推 SSE step 事件 —— 前端步骤可视化与「平均步数 / 降级率 / 步级延迟」口径的唯一来源；
     * 端口实现内部兜底、不打挂主链路。 */
    private void recordToolStep(
            Spec spec, String traceId, int stepIndex, ToolLoopCall call, DispatchResult result, long latencyMs) {
        tracePort.record(new ToolCallStepTrace(
                traceId,
                spec.sessionId(),
                spec.userId(),
                stepIndex,
                call.tool(),
                call.toolInput(),
                call.thought(),
                result.observation(),
                latencyMs,
                result.succeeded(),
                result.errorMsg()));
        emitStep(spec, stepIndex, call, result.observation());
    }

    /** 落一步 finish trace —— 收敛轮没有执行体，入参与观察为空、延迟记 0、视为成功。 */
    private void recordFinishStep(Spec spec, String traceId, int stepIndex, ToolLoopCall finish) {
        tracePort.record(new ToolCallStepTrace(
                traceId,
                spec.sessionId(),
                spec.userId(),
                stepIndex,
                finish.tool(),
                null,
                finish.thought(),
                null,
                0,
                true,
                null));
        emitStep(spec, stepIndex, finish, null);
    }

    private void emitStep(Spec spec, int stepIndex, ToolLoopCall call, @Nullable String observation) {
        ChatStreamHandler streamHandler = spec.streamHandler();
        if (streamHandler != null) {
            streamHandler.onStep(new ToolCallStepView(stepIndex, call.tool(), call.thought(), observation));
        }
    }

    /** 一轮的执行结果 — nextStepIndex 跨轮连续（1 起）；toolPath 含 finish 轮，路径上「模型选了什么」保持完整。 */
    private record RoundResult(int nextStepIndex, List<String> toolPath, boolean finished) {}

    private record DispatchResult(boolean succeeded, String observation) {

        @Nullable
        String errorMsg() {
            return succeeded ? null : observation;
        }
    }

    /**
     * 工具面（一次请求内）— 按名执行对照表 + 未知工具的可用清单；清单按本次真正下发的回调生成，白名单
     * 过滤后不会报出模型拿不到的工具。召回累加器归各链路工具面实例，内核不持召回状态。
     */
    private record ToolDispatcher(Map<String, ToolCallback> toolsByName, String availableToolNames) {

        static ToolDispatcher of(List<ToolCallback> callbacks) {
            var toolsByName = callbacks.stream()
                    .collect(Collectors.toMap(
                            callback -> callback.getToolDefinition().name(), Function.identity()));
            // 单独存串而不从 toolsByName 派生：HashMap 迭代序与下发顺序无关，模型看到的工具清单须按下发序保持稳定
            String availableToolNames = callbacks.stream()
                    .map(callback -> callback.getToolDefinition().name())
                    .collect(Collectors.joining(" / "));
            return new ToolDispatcher(toolsByName, availableToolNames);
        }

        /** 未知工具与执行异常（参数不合 schema / 工具内部故障）都收敛成失败观察：模型据此重试或收敛，不把整轮对话打死。 */
        DispatchResult dispatch(ToolLoopCall call) {
            ToolCallback callback = toolsByName.get(call.tool());
            if (callback == null) {
                return new DispatchResult(false, "未知工具 %s，请改用 %s".formatted(call.tool(), availableToolNames));
            }
            try {
                return new DispatchResult(true, callback.call(call.rawArguments()));
            } catch (Exception e) {
                // MethodToolCallback 把「参数转换失败」与「方法体异常」统一包成 ToolExecutionException
                String reason = FailureReason.of(e.getCause() != null ? e.getCause() : e);
                log.warn(
                        "action=tool_call_failed, tool={}, input={}, reason={}", call.tool(), call.toolInput(), reason);
                return new DispatchResult(false, reason);
            }
        }
    }
}
