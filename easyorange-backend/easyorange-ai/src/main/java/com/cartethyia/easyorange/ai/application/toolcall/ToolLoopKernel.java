package com.cartethyia.easyorange.ai.application.toolcall;

import com.cartethyia.easyorange.ai.application.support.FailureText;
import com.cartethyia.easyorange.ai.domain.model.ToolCallStepTrace;
import com.cartethyia.easyorange.ai.domain.model.ToolCallStepView;
import com.cartethyia.easyorange.ai.domain.model.ToolLoopOutcomeView;
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
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * 多步工具调用循环（ReAct）的编排内核 —— 逐轮「决策 → 工具 → 观察」，推进到模型判定信息足够为止。
 * <p>
 * 在链路里的位置：被两条链路的编排器调用（买家 {@code ChatToolLoop} / 卖家 {@code AutoListingAppService}），
 * 只持链路无关的机制：for 循环与四条出口、协议回填、步级 trace / SSE、toolPath。首轮上下文、每轮决策函数、模型路由、
 * 记账场景与降级动作全由 {@link Spec} 注入 —— 内核不认业务语义，两条链路因此共用一份实现。
 * <p>
 * 手写循环而非 {@code ChatClient} 自动循环：{@code ChatModel.call} 不自动执行工具，轮数上限与降级控制权必须留在
 * 本类；trace / step / 指标是观测副产物，失败不碰主链路。
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
     * 一次循环的规格 —— 两条链路的差异全在这里注入（含每轮的决策函数），内核不认识业务语义。
     * <p>
     * 十一项分三类：① 首轮上下文（{@code systemPrompt} / {@code firstUserMessage}）；② 每轮要用什么
     * （{@code toolCallbacks} / {@code maxRounds} / {@code roundDecider}）；③ 出事与观测（{@code budgetExhausted}
     * 每轮问一次、{@code decisionFailedFallback} 决策失败时调、{@code sessionId} / {@code userId} /
     * {@code streamHandler} / {@code listener}）。
     * <p>
     * {@code userId} 是已归属的值（评估跑批传 null），归属判定留在各链路编排器（同一口径还管工具面的偏好归属）。
     */
    public record Spec(
            String systemPrompt,
            String firstUserMessage,
            List<ToolCallback> toolCallbacks,
            int maxRounds,
            RoundDecider roundDecider,
            BooleanSupplier budgetExhausted,
            /* 决策失败出口的降级动作（chat 按原始问题补检索一次、listing 无操作），异常自担不外抛。 */
            Runnable decisionFailedFallback,
            @Nullable String sessionId,
            @Nullable String userId,
            @Nullable ChatStreamHandler streamHandler,
            ToolLoopListener listener) {}

    /** 循环结果 —— 从哪条出口出来（outcome）、跑了几轮（rounds）、按顺序调过哪些工具（toolPath，含 finish 轮）。前两项供指标与降级归因，toolPath 供路由准确率评估对照金标准集。 */
    public record Result(ToolLoopOutcome outcome, int rounds, List<String> toolPath) {}

    /** 入口：跑一次完整循环并回报结局；异常原样抛出，但先把「故障」记进指标（它没有循环出口、轮数不可知）。 */
    public Result run(Spec spec) {
        try {
            Result result = executeLoop(spec);
            spec.listener().recordLoop(result.outcome(), result.rounds());
            // 四条出口在这里汇聚，推给流式侧也只在这一处——与上面那行指标同读一个 Result，两边不会各判各的
            ChatStreamHandler handler = spec.streamHandler();
            if (handler != null) {
                handler.onLoopOutcome(new ToolLoopOutcomeView(result.outcome().getTag(), result.rounds()));
            }
            return result;
        } catch (RuntimeException e) {
            spec.listener().recordLoopFailure();
            throw e;
        }
    }

    /** 主循环 —— 四条出口都在这里：预算尽 / 决策失败 / 模型收敛（finish）/ 轮数用尽。 */
    private Result executeLoop(Spec spec) {
        String traceId = idGenerator.generateId();
        var dispatcher = ToolDispatcher.of(spec.toolCallbacks());
        var transcript = new ToolLoopTranscript(spec.systemPrompt(), spec.firstUserMessage());
        var toolPath = new ArrayList<String>();
        int rounds = 0;
        int nextStepIndex = 1;

        for (int round = 1; round <= spec.maxRounds(); round++) {
            if (round > 1 && spec.budgetExhausted().getAsBoolean()) {
                log.warn(
                        "action=tool_call_loop_degraded, reason=budget, sessionId={}, rounds={}",
                        spec.sessionId(),
                        rounds);
                return new Result(ToolLoopOutcome.BUDGET, rounds, List.copyOf(toolPath));
            }
            List<ToolCallStep> steps =
                    spec.roundDecider().nextSteps(spec.sessionId(), transcript.messages(), spec.toolCallbacks());
            if (steps.isEmpty()) {
                spec.decisionFailedFallback().run();
                return new Result(ToolLoopOutcome.DECISION_FAILED, rounds, List.copyOf(toolPath));
            }
            rounds = round;
            RoundResult roundResult = executeSteps(spec, traceId, dispatcher, transcript, steps, nextStepIndex);
            nextStepIndex = roundResult.nextStepIndex();
            toolPath.addAll(roundResult.toolPath());
            if (roundResult.finished()) {
                return new Result(ToolLoopOutcome.FINISHED, rounds, List.copyOf(toolPath));
            }
        }
        log.warn(
                "action=tool_call_loop_degraded, reason=step_limit, sessionId={}, rounds={}, toolPath={}",
                spec.sessionId(),
                rounds,
                String.join(",", toolPath));
        return new Result(ToolLoopOutcome.ROUND_LIMIT, rounds, List.copyOf(toolPath));
    }

    /**
     * 执行一轮的全部调用，落成观测副产物（trace / SSE step / 步级指标），再按对话协议回填。步序跨轮连续，
     * 否则一轮内多个工具挤进同一 stepIndex，trace 里两个动作看起来是同一步。
     */
    private RoundResult executeSteps(
            Spec spec,
            String traceId,
            ToolDispatcher dispatcher,
            ToolLoopTranscript transcript,
            List<ToolCallStep> steps,
            int firstStepIndex) {
        // finish 先摘出去：执行体里就没有「跳过它」的分支，回填的 tool_calls 与观察天然等长；多个 finish 取最后一个
        ToolCallStep finish = null;
        var executableSteps = new ArrayList<ToolCallStep>(steps.size());
        for (ToolCallStep step : steps) {
            if (FINISH_TOOL.equals(step.tool())) {
                finish = step;
            } else {
                executableSteps.add(step);
            }
        }

        var toolPath = new ArrayList<String>(executableSteps.size() + 1);
        var observations = new ArrayList<String>(executableSteps.size());
        int stepIndex = firstStepIndex;
        for (ToolCallStep step : executableSteps) {
            DispatchResult result = executeOneTool(spec, traceId, stepIndex++, dispatcher, step);
            toolPath.add(step.tool());
            observations.add(result.observation());
        }
        if (finish != null) {
            recordFinishStep(spec, traceId, stepIndex, finish);
            toolPath.add(finish.tool());
            return new RoundResult(stepIndex + 1, toolPath, true);
        }
        transcript.appendRound(executableSteps, observations);
        return new RoundResult(stepIndex, toolPath, false);
    }

    /** 执行单个工具：计时、记工具指标、落一步 trace 与 SSE。 */
    private DispatchResult executeOneTool(
            Spec spec, String traceId, int stepIndex, ToolDispatcher dispatcher, ToolCallStep step) {
        long start = System.nanoTime();
        DispatchResult result = dispatcher.dispatch(step);
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        spec.listener().recordTool(step.tool(), latencyMs);
        recordToolStep(spec, traceId, stepIndex, step, result, latencyMs);
        return result;
    }

    /** 落一步工具步 trace 并推 SSE step 事件 —— 前端步骤可视化与「工具步数 / 步级延迟 p95」口径的唯一来源；
     * 端口实现内部兜底、不打挂主链路。 */
    private void recordToolStep(
            Spec spec, String traceId, int stepIndex, ToolCallStep step, DispatchResult result, long latencyMs) {
        tracePort.record(new ToolCallStepTrace(
                traceId,
                spec.sessionId(),
                spec.userId(),
                stepIndex,
                step.tool(),
                step.toolInput(),
                step.thought(),
                result.observation(),
                latencyMs,
                result.succeeded(),
                result.errorMsg()));
        emitStep(spec, stepIndex, step, result.observation());
    }

    /** 落一步 finish trace —— 收敛轮没有执行体，入参与观察为空、延迟记 0、视为成功。 */
    private void recordFinishStep(Spec spec, String traceId, int stepIndex, ToolCallStep finish) {
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

    /** 推一条 SSE step 事件；非流式路径 streamHandler 为 null，直接跳过。 */
    private void emitStep(Spec spec, int stepIndex, ToolCallStep step, @Nullable String observation) {
        ChatStreamHandler streamHandler = spec.streamHandler();
        if (streamHandler != null) {
            streamHandler.onStep(new ToolCallStepView(stepIndex, step.tool(), step.thought(), observation));
        }
    }

    /** 一轮的执行结果 — nextStepIndex 跨轮连续（1 起）；toolPath 含 finish 轮，路径上「模型选了什么」保持完整。 */
    private record RoundResult(int nextStepIndex, List<String> toolPath, boolean finished) {}

    /** 一次工具执行的结果 —— 成败 + 给模型看的观察文本；失败时观察文本即错误原因。 */
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
        DispatchResult dispatch(ToolCallStep step) {
            ToolCallback callback = toolsByName.get(step.tool());
            if (callback == null) {
                return new DispatchResult(false, "未知工具 %s，请改用 %s".formatted(step.tool(), availableToolNames));
            }
            try {
                return new DispatchResult(true, callback.call(step.rawArguments()));
            } catch (Exception e) {
                // MethodToolCallback 把「参数转换失败」与「方法体异常」统一包成 ToolExecutionException
                String reason = FailureText.of(e.getCause() != null ? e.getCause() : e);
                log.warn(
                        "action=tool_call_failed, tool={}, input={}, reason={}", step.tool(), step.toolInput(), reason);
                return new DispatchResult(false, reason);
            }
        }
    }
}
