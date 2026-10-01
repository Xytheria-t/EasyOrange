package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.support.ChatBudgetGuard;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.ToolCallStepTrace;
import com.cartethyia.easyorange.ai.domain.model.ToolCallStepView;
import com.cartethyia.easyorange.ai.domain.model.UserPreference;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistryPort;
import com.cartethyia.easyorange.ai.domain.port.ToolCallStepTracePort;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * 多步工具调用循环（ReAct）— 逐轮「决策 → 工具 → 观察」推进到模型判定信息足够。
 * <p>
 * 手写循环 + 原生 tool calling：{@code ChatModel.call} 不自动执行工具（Spring AI 2.0 删掉了
 * internalToolExecutionEnabled、模型类无 executeToolCalls 调用点，代执行上移到 ChatClient），步数与
 * 降级控制权留在本类；一轮可多调用同轮全执行、按模型顺序逐个执行（召回累加器是实例独占状态）。
 * <p>
 * 降级口径（退回确定性单次生成，已积累观察不丢弃）：步数/预算超限 → 用已积累观察直接生成；决策失败 →
 * 按原始问题补一次检索；工具执行失败≠决策失败，收敛成失败观察交回模型自修复。trace/step/指标是副产物，失败不碰主链路。
 */
@Slf4j
@Component
public class ToolCallLoop {

    /** 决策对话的 system prompt 键（与 {@code prompts/ai_chat_tool.yml} 的 name 同名）。 */
    private static final String CHAT_TOOL_PROMPT = "ai_chat_tool_system";
    /** 未知工具观察里的工具名清单（与 {@link ChatTools} 的常量同源，不重写字面量）。 */
    private static final String TOOL_NAME_LIST = String.join(
            " / ",
            ChatTools.TOOL_KNOWLEDGE_SEARCH,
            ChatTools.TOOL_PRODUCT_SEARCH,
            ChatTools.TOOL_PRODUCT_DETAIL,
            ChatTools.TOOL_MARKET_PRICE_STATS,
            ChatTools.TOOL_COMPARE_ASSETS,
            ChatTools.TOOL_REMEMBER_PREFERENCE,
            ChatTools.TOOL_FINISH);

    /**
     * 机器主体标识 —— 唯一非登录调用方是评估跑批（{@code GoldenSetEvaluator}）：会话 / 缓存键照常按此主体隔离，但画像不
     * 落库、trace 的 user_id 为空。HTTP 入口无匿名路径（身份缺失即 401），故「机器主体」≠「匿名用户」。
     */
    public static final String MACHINE_SUBJECT = "machine";

    private final PromptRegistryPort promptRegistry;
    private final ChatToolsFactory toolsFactory;
    private final ToolCallDecider decider;
    private final ToolCallStepTracePort tracePort;
    private final ChatBudgetGuard budgetGuard;
    private final AiProperties aiProperties;
    private final IdGenerator idGenerator;
    private final ToolCallLoopMetrics metrics;

    public ToolCallLoop(
            PromptRegistryPort promptRegistry,
            ChatToolsFactory toolsFactory,
            ToolCallDecider decider,
            ToolCallStepTracePort tracePort,
            ChatBudgetGuard budgetGuard,
            AiProperties aiProperties,
            IdGenerator idGenerator,
            ToolCallLoopMetrics metrics) {
        this.promptRegistry = promptRegistry;
        this.toolsFactory = toolsFactory;
        this.decider = decider;
        this.tracePort = tracePort;
        this.budgetGuard = budgetGuard;
        this.aiProperties = aiProperties;
        this.idGenerator = idGenerator;
        this.metrics = metrics;
    }

    /**
     * 一次循环的输入 — 记忆（历史 / 画像）由调用方装配，循环只管「决策 → 工具 → 观察」。
     *
     * @param sessionId 可空（请求不带会话），两处消费方各自兜底：trace 落 anonymous 桶、会话记忆无会话 fail-open
     * @param userId    评估跑批传 {@link #MACHINE_SUBJECT}，画像不落库
     * @param handler   可空：非流式路径不推 step 事件，trace / 指标照常
     * @param toolAllowList 工具白名单，null = 全量工具面（生产路径）；非空则只装配列出的工具，供
     *                      RAG 有效性对照的「无检索来源」臂只挂 {@link ChatTools#TOOL_FINISH} ——
     *                      消融变量只落在工具装配上，编排、生成与 Judge 口径两臂完全同构
     */
    public record Input(
            String question,
            @Nullable String sessionId,
            String userId,
            List<ChatTurn> history,
            List<UserPreference> prefs,
            @Nullable ChatStreamHandler handler,
            @Nullable Set<String> toolAllowList) {

        public Input(
                String question,
                @Nullable String sessionId,
                String userId,
                List<ChatTurn> history,
                List<UserPreference> prefs,
                @Nullable ChatStreamHandler handler) {
            this(question, sessionId, userId, history, prefs, handler, null);
        }
    }

    /**
     * 循环结果 — 召回物供最终生成装配 prompt 与引用溯源；outcome / rounds 供指标与降级归因；toolPath 供路由准确率评估
     * （金标准集 {@code expected_tools} 对照）：rounds 含 finish 轮但不计决策失败轮，降级补检索由代码发起、不进 toolPath。
     */
    public record Result(
            List<KnowledgeHit> knowledgeHits,
            List<AssetHit> assetHits,
            List<AssetDetail> details,
            ToolCallLoopOutcome outcome,
            int rounds,
            List<String> toolPath) {

        /** 出循环时一次性定稿 — 三个召回物来自 {@code tools} 实例上已积累的累加器，循环只有这一处读它。 */
        static Result of(ChatTools tools, ToolCallLoopOutcome outcome, int rounds, List<String> toolPath) {
            return new Result(
                    tools.knowledgeHits(), tools.assetHits(), tools.details(), outcome, rounds, List.copyOf(toolPath));
        }
    }

    public Result run(Input input) {
        try {
            Result result = executeLoop(input);
            metrics.recordLoop(result.outcome(), result.rounds());
            return result;
        } catch (RuntimeException e) {
            metrics.recordLoopFailure();
            throw e;
        }
    }

    private Result executeLoop(Input input) {
        String traceId = idGenerator.generateId();
        var tools = toolsFactory.create(attributedUserId(input));
        var dispatcher = ToolDispatcher.of(tools, input.toolAllowList());
        var messages = new DecisionMessages(promptRegistry.require(CHAT_TOOL_PROMPT), firstUserMessage(input));
        var toolPath = new ArrayList<String>();
        int rounds = 0;
        int nextStepIndex = 1;

        for (int round = 1; round <= aiProperties.chat().maxSteps(); round++) {
            if (round > 1 && budgetGuard.exhausted()) {
                log.warn(
                        "action=tool_call_loop_degraded, reason=budget, sessionId={}, rounds={}",
                        input.sessionId(),
                        rounds);
                return Result.of(tools, ToolCallLoopOutcome.BUDGET, rounds, toolPath);
            }
            List<ToolCallDecision> decisions =
                    decider.decide(input.sessionId(), messages.snapshot(), dispatcher.callbacks());
            if (decisions.isEmpty()) {
                // 识别不出检索需求时仍补一次：最坏是多几条不相关片段，好过把检索链路失效伪装成「无需检索」
                // 白名单未含知识库检索（无检索对照臂）时跳过：补检索会把这臂的检索来源偷偷加回来，对照失效
                if (allowsKnowledgeSearch(input)) {
                    try {
                        tools.searchKnowledgeForFallback(input.question());
                    } catch (Exception e) {
                        log.warn(
                                "action=tool_call_fallback_search_failed, sessionId={}, reason={}",
                                input.sessionId(),
                                FailureReason.of(e));
                    }
                }
                return Result.of(tools, ToolCallLoopOutcome.DECISION_FAILED, rounds, toolPath);
            }
            rounds = round;
            RoundResult roundResult = executeToolCalls(input, traceId, dispatcher, messages, decisions, nextStepIndex);
            nextStepIndex = roundResult.nextStepIndex();
            toolPath.addAll(roundResult.toolPath());
            if (roundResult.finished()) {
                return Result.of(tools, ToolCallLoopOutcome.FINISHED, rounds, toolPath);
            }
        }
        log.warn(
                "action=tool_call_loop_degraded, reason=step_limit, sessionId={}, rounds={}, toolPath={}",
                input.sessionId(),
                rounds,
                String.join(",", toolPath));
        return Result.of(tools, ToolCallLoopOutcome.STEP_LIMIT, rounds, toolPath);
    }

    /**
     * 执行一轮里的全部工具调用并落成观测副产物（trace 落库 / SSE step 事件 / 步级指标），再按对话协议回填。与
     * {@link ToolCallDecider#decide} 同以「这批工具调用」为宾语：轮是循环级单位，不写进方法名（否则与 {@code
     * recordToolStep} 的「步」分不开）；步序跨轮连续，否则一轮内的多个工具（同一个决策动作）挤进同一 stepIndex 会让
     * trace 里两个动作看起来是同一步。
     */
    private RoundResult executeToolCalls(
            Input input,
            String traceId,
            ToolDispatcher dispatcher,
            DecisionMessages messages,
            List<ToolCallDecision> decisions,
            int firstStepIndex) {
        // finish 先摘出去：执行体里就没有「跳过它」的分支，回填的 tool_calls 与观察天然等长；多个 finish 取最后一个
        ToolCallDecision finish = null;
        var executableCalls = new ArrayList<ToolCallDecision>(decisions.size());
        for (ToolCallDecision decision : decisions) {
            if (decision.isFinish()) {
                finish = decision;
            } else {
                executableCalls.add(decision);
            }
        }

        var toolPath = new ArrayList<String>(executableCalls.size() + 1);
        var observations = new ArrayList<String>(executableCalls.size());
        int stepIndex = firstStepIndex;
        for (ToolCallDecision decision : executableCalls) {
            ToolResult result = executeOneTool(input, traceId, stepIndex++, dispatcher, decision);
            toolPath.add(decision.tool());
            observations.add(result.observation());
        }
        if (finish != null) {
            recordFinishStep(input, traceId, stepIndex, finish);
            toolPath.add(ChatTools.TOOL_FINISH);
            return new RoundResult(stepIndex + 1, toolPath, true);
        }
        messages.appendRound(rawToolCallsOf(executableCalls), observations);
        return new RoundResult(stepIndex, toolPath, false);
    }

    private ToolResult executeOneTool(
            Input input, String traceId, int stepIndex, ToolDispatcher dispatcher, ToolCallDecision decision) {
        long start = System.nanoTime();
        ToolResult result = dispatcher.dispatch(decision);
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        metrics.recordTool(decision.tool(), latencyMs);
        recordToolStep(input, traceId, stepIndex, decision, toolInputOf(decision), result, latencyMs);
        return result;
    }

    /** 按模型给出的顺序取原始 tool call —— 回填时 assistant 与 role=tool 两侧须同序。 */
    private static List<AssistantMessage.ToolCall> rawToolCallsOf(List<ToolCallDecision> decisions) {
        return decisions.stream().map(ToolCallDecision::rawToolCall).toList();
    }

    /** 落一步工具步 trace 并推 SSE step 事件 —— 前端步骤可视化与「平均步数 / 降级率 / 步级延迟」口径的唯一数据来源，
     * 端口实现内部兜底、不打挂主链路。 */
    private void recordToolStep(
            Input input,
            String traceId,
            int stepIndex,
            ToolCallDecision decision,
            String toolInput,
            ToolResult result,
            long latencyMs) {
        tracePort.record(new ToolCallStepTrace(
                traceId,
                input.sessionId(),
                attributedUserId(input),
                stepIndex,
                decision.tool(),
                toolInput,
                decision.parsedArguments().thought(),
                result.observation(),
                latencyMs,
                result.succeeded(),
                result.errorMsg()));
        emitStep(input, stepIndex, decision, result.observation());
    }

    /** 落一步 finish trace —— 收敛轮没有执行体，入参与观察为空、延迟记 0、视为成功。不与工具步共用带可空参数的落库方法：
     * 结果类型收成非空，需要判断「无执行体」的只有调用点本身。 */
    private void recordFinishStep(Input input, String traceId, int stepIndex, ToolCallDecision finish) {
        tracePort.record(new ToolCallStepTrace(
                traceId,
                input.sessionId(),
                attributedUserId(input),
                stepIndex,
                finish.tool(),
                null,
                finish.parsedArguments().thought(),
                null,
                0,
                true,
                null));
        emitStep(input, stepIndex, finish, null);
    }

    private void emitStep(Input input, int stepIndex, ToolCallDecision decision, @Nullable String observation) {
        ChatStreamHandler handler = input.handler();
        if (handler != null) {
            handler.onStep(new ToolCallStepView(
                    stepIndex, decision.tool(), decision.parsedArguments().thought(), observation));
        }
    }

    /** 一轮的执行结果 — nextStepIndex 跨轮连续（1 起）；toolPath 含 finish 轮，让整条路径上的「模型选了什么」完整。 */
    private record RoundResult(int nextStepIndex, List<String> toolPath, boolean finished) {}

    private record ToolResult(boolean succeeded, String observation) {

        @Nullable
        String errorMsg() {
            return succeeded ? null : observation;
        }
    }

    /** 工具面（一次请求内） — 两种框架形态（schema 下发的回调列表、按名执行的回调表）绑在一处按名分发；召回累加器归 {@link ChatTools} 实例。 */
    private record ToolDispatcher(List<ToolCallback> callbacks, Map<String, ToolCallback> byName) {

        /** 白名单为 null 时装配全量工具面；否则只装配白名单内的工具（模型拿不到 schema 就调不到）。 */
        static ToolDispatcher of(ChatTools tools, @Nullable Set<String> allowList) {
            List<ToolCallback> callbacks = List.of(ToolCallbacks.from(tools)).stream()
                    .filter(callback -> allowList == null
                            || allowList.contains(callback.getToolDefinition().name()))
                    .toList();
            var byName = callbacks.stream()
                    .collect(Collectors.toMap(
                            callback -> callback.getToolDefinition().name(), Function.identity()));
            return new ToolDispatcher(callbacks, byName);
        }

        /** 未知工具与执行异常（参数不合 schema / 工具内部故障）都收敛成失败观察：模型据此重试或收敛，不把整轮对话打死。 */
        ToolResult dispatch(ToolCallDecision decision) {
            String tool = decision.tool();
            ToolCallback callback = byName.get(tool);
            if (callback == null) {
                return new ToolResult(false, "未知工具 %s，请改用 %s".formatted(tool, TOOL_NAME_LIST));
            }
            try {
                return new ToolResult(true, callback.call(decision.rawArguments()));
            } catch (Exception e) {
                // MethodToolCallback 把「参数转换失败」与「方法体异常」统一包成 ToolExecutionException
                String reason = FailureReason.of(e.getCause() != null ? e.getCause() : e);
                log.warn("action=tool_call_failed, tool={}, input={}, reason={}", tool, toolInputOf(decision), reason);
                return new ToolResult(false, reason);
            }
        }
    }

    @Nullable
    private static String attributedUserId(Input input) {
        return MACHINE_SUBJECT.equals(input.userId()) ? null : input.userId();
    }

    private static boolean allowsKnowledgeSearch(Input input) {
        Set<String> allowList = input.toolAllowList();
        return allowList == null || allowList.contains(ChatTools.TOOL_KNOWLEDGE_SEARCH);
    }

    /**
     * 首条 user 消息（问题 / 历史 / 画像）— 每请求固定不变，是全部轮次共享的前缀：改一个字节这轮的 KV cache 就全部作废。
     * 三个分量都过 {@link UntrustedText#stripTags}：这条上下文决定调哪个工具（含唯一写路径 remember_preference），原样
     * 填等于把闭合标签的注入口留在决策侧。
     */
    private static String firstUserMessage(Input input) {
        return """
                用户问题：
                <user_question>
                %s
                </user_question>

                历史对话：
                %s

                用户画像：
                %s
                """.formatted(
                        UntrustedText.stripTags(input.question()),
                        UntrustedText.stripTags(formatHistory(input.history())),
                        UntrustedText.stripTags(UserPreference.format(input.prefs())));
    }

    private static String formatHistory(List<ChatTurn> history) {
        if (history.isEmpty()) {
            return "(无)";
        }
        return history.stream()
                .map(turn -> (turn.role().isUser() ? "用户" : "助手") + ": " + turn.content())
                .collect(Collectors.joining("\n"));
    }

    /**
     * 工具入参摘要（trace 落库与失败日志用）—— 按工具名取对应分量，其余工具分量为 null 是常态。工具名缺失已在
     * {@link ToolCallDecision} 构造期收敛成空串，这里走 default 即可：再判一次空等于同一件事防两遍。
     */
    private static String toolInputOf(ToolCallDecision decision) {
        ToolCallArguments parsed = decision.parsedArguments();
        return switch (decision.tool()) {
            case ChatTools.TOOL_PRODUCT_DETAIL -> parsed.productId();
            case ChatTools.TOOL_COMPARE_ASSETS ->
                parsed.productIds() == null ? null : String.join("、", parsed.productIds());
            case ChatTools.TOOL_REMEMBER_PREFERENCE ->
                orEmpty(parsed.preferenceKey()) + "=" + orEmpty(parsed.preferenceValue());
            default -> parsed.query();
        };
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }
}
