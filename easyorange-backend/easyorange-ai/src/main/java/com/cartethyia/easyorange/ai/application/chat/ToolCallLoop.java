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
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import com.cartethyia.easyorange.ai.domain.port.ToolCallStepTracePort;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * 多步工具调用循环（ReAct）— 逐轮「决策 → 工具 → 观察」推进，直到模型判定信息足够（finish）。
 * <p>
 * 手写循环 + 原生 tool calling：每轮把 7 个工具的 JSON Schema（{@link ChatTools} 的 {@code @Tool}
 * 注解生成）随请求下发，模型返回「调哪个工具 + 参数 + 理由」。调不调、调几次、调什么由本类决定
 * —— Spring AI 2.0 的 {@code ChatModel.call} 不自动执行工具，步数与降级的循环控制权留在本类
 * （预算余量问 {@link ChatBudgetGuard}，本类只决定何时因它切断）。
 * <p>
 * 一轮可以带多个工具调用：供应商侧并行发起（典型是「规则 + 找货」兼有的问题同时要规则知识与在售资产）
 * 时同轮全部执行，省掉一整轮决策往返 —— 决策调用比工具调用贵一个量级，砍往返比并发执行工具划算得多。
 * 同轮多个调用按模型给出的顺序逐个执行（召回累加器是实例独占的可变状态，见 {@link ChatTools}）——
 * 顺序因此带语义：唯一「读累加器却不带参数」的 market_price_stats 排在 product_search 前面会统计到空集。
 * 本类不做依赖排序，约束落在该工具的 schema 描述里（模型选工具时就读得到）；空集观察可恢复，只多一轮。
 * finish 与其它调用同现时以 finish 收敛：同轮非 finish 调用照常执行（模型确实要了这份信息），
 * finish 记在最后一个；收敛后不回填消息（本轮序列就此丢弃）。
 * <p>
 * 降级口径（自治循环被切断，退回确定性单次生成，已积累的观察不丢弃）：步数超限 / 预算超限
 * （与入口预检同一判定，见 {@link ChatBudgetGuard}）→ 用已积累观察直接生成；决策失败 → 按原始问题补一次
 * 检索后直接生成。工具执行失败不算决策失败：收敛成失败观察交回模型修复，不打断对话。
 * <p>
 * 每轮 trace 落库、每步推 SSE step 事件、每次循环计指标 —— 三者都是观测副产物，失败绝不影响主链路。
 * 指标落在 {@link ToolCallLoopMetrics}、预算判定落在 {@link ChatBudgetGuard}：本类只管「决策 → 工具 → 观察」，
 * 不知道指标名与 tag 契约，也不管这次调用还发不发得出去。
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
     * 机器主体的统一标识 —— 唯一非登录调用方是评估跑批（{@code GoldenSetEvaluator} 定时回归 / CI 门禁）：
     * 会话 / 缓存键照常按此主体隔离，但画像不落库（{@code ChatTools} 拒写）、trace 的 user_id 为空
     * （{@link #attributedUserId} 收敛）。对话的 HTTP 入口不存在匿名路径 —— 身份缺失即 401，
     * 所以「机器主体」不等于「匿名用户」。哨兵值定义在本类（唯一判定画像是否落库的地方），
     * 调用方只负责把该主体传进来。
     */
    public static final String MACHINE_SUBJECT = "machine";

    private final PromptRegistry promptRegistry;
    private final ChatToolsFactory toolsFactory;
    private final ToolCallDecider decider;
    private final ToolCallStepTracePort tracePort;
    private final ChatBudgetGuard budgetGuard;
    private final AiProperties aiProperties;
    private final IdGenerator idGenerator;
    private final ToolCallLoopMetrics metrics;

    public ToolCallLoop(
            PromptRegistry promptRegistry,
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
     * @param sessionId 会话 ID，可空：请求不带会话时为 null。两处消费方各自兜底（trace 落 anonymous
     *                 桶、会话记忆按无会话 fail-open），本类只把它带进日志与 trace
     * @param userId    登录用户 ID；评估跑批（非登录调用方）传 {@link #MACHINE_SUBJECT}，画像不落库
     * @param handler   流式回调，可空：非流式路径不推 step 事件，trace / 指标照常
     */
    public record Input(
            String question,
            @Nullable String sessionId,
            String userId,
            List<ChatTurn> history,
            List<UserPreference> prefs,
            @Nullable ChatStreamHandler handler) {}

    /**
     * 循环结果 — 召回物供最终生成装配 prompt 与引用溯源；outcome / rounds 供指标与降级归因；
     * toolPath 是模型实际选过的工具序列，供路由准确率评估（金标准集 {@code expected_tools} 对照）。
     * rounds 含 finish 轮，决策失败轮不计（那一轮没有决策）；toolPath 同样不含降级补检索
     * ——补检索由代码发起，不是模型的路由决策。
     */
    public record Result(
            List<KnowledgeHit> knowledgeHits,
            List<AssetHit> assetHits,
            List<AssetDetail> details,
            ToolCallLoopOutcome outcome,
            int rounds,
            List<String> toolPath) {}

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
        var dispatcher = ToolDispatcher.of(tools);
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
                return toResult(tools, ToolCallLoopOutcome.BUDGET, rounds, toolPath);
            }
            List<ToolCallDecision> decisions =
                    decider.decide(input.sessionId(), messages.snapshot(), dispatcher.callbacks());
            if (decisions.isEmpty()) {
                // 识别不出检索需求时仍补一次：最坏是多几条不相关片段，好过把检索链路失效伪装成「无需检索」
                try {
                    tools.searchKnowledgeForFallback(input.question());
                } catch (Exception e) {
                    log.warn(
                            "action=tool_call_fallback_search_failed, sessionId={}, reason={}",
                            input.sessionId(),
                            failureReason(e));
                }
                return toResult(tools, ToolCallLoopOutcome.DECISION_FAILED, rounds, toolPath);
            }
            rounds = round;
            RoundResult roundResult = executeToolCalls(input, traceId, dispatcher, messages, decisions, nextStepIndex);
            nextStepIndex = roundResult.nextStepIndex();
            toolPath.addAll(roundResult.toolPath());
            if (roundResult.finished()) {
                return toResult(tools, ToolCallLoopOutcome.FINISHED, rounds, toolPath);
            }
        }
        log.warn(
                "action=tool_call_loop_degraded, reason=step_limit, sessionId={}, rounds={}, toolPath={}",
                input.sessionId(),
                rounds,
                String.join(",", toolPath));
        return toResult(tools, ToolCallLoopOutcome.STEP_LIMIT, rounds, toolPath);
    }

    /** 出循环时一次性定稿：结局 + 轮数 + 工具路径 + 工具实例上已积累的召回物。 */
    private static Result toResult(ChatTools tools, ToolCallLoopOutcome outcome, int rounds, List<String> toolPath) {
        return new Result(
                tools.knowledgeHits(), tools.assetHits(), tools.details(), outcome, rounds, List.copyOf(toolPath));
    }

    /**
     * 执行一轮里的全部工具调用并落成观测副产物（trace 落库 / SSE step 事件 / 步级指标），再按对话协议回填。
     * 与 {@link ToolCallDecider#decide} 同以「这批工具调用」为宾语：轮是循环级单位（{@code round} 循环变量与
     * {@link RoundResult} 归它），不写进方法名 —— 否则与 {@code recordToolStep} 的「步」在名字上分不开。
     * 步序跨轮连续（{@code firstStepIndex} 进、{@link RoundResult#nextStepIndex()} 出）：一轮内的并行调用是同一个决策
     * 动作的多个工具，挤进同一个 stepIndex 会让 trace 里两个动作看起来是同一步。
     */
    private RoundResult executeToolCalls(
            Input input,
            String traceId,
            ToolDispatcher dispatcher,
            DecisionMessages messages,
            List<ToolCallDecision> decisions,
            int firstStepIndex) {
        // finish 先摘出去：执行体里就没有「跳过它」的分支，回填的 tool_calls 与观察也天然等长。
        // 一趟分完而不是两趟 filter：同一份 decisions 只走一次；多个 finish 取最后一个，与摘出前的行为一致
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

    /** 执行一步工具调用，顺带把这步的观测副产物记全（步级延迟指标 / trace / SSE step 事件）—— 循环里只留骨架。 */
    private ToolResult executeOneTool(
            Input input, String traceId, int stepIndex, ToolDispatcher dispatcher, ToolCallDecision decision) {
        long start = System.nanoTime();
        ToolResult result = dispatcher.dispatch(decision);
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        metrics.recordTool(decision.tool(), latencyMs);
        recordToolStep(input, traceId, stepIndex, decision, toolInputOf(decision), result, latencyMs);
        return result;
    }

    /** 本轮全部 tool call 的原始对象（按模型给出的顺序）—— 回填时 assistant 与 role=tool 两侧同序。 */
    private static List<AssistantMessage.ToolCall> rawToolCallsOf(List<ToolCallDecision> decisions) {
        return decisions.stream().map(ToolCallDecision::rawToolCall).toList();
    }

    /**
     * 落一步工具步 trace 并推 SSE step 事件 —— 前端步骤可视化与「平均步数 / 降级率 / 步级延迟」口径的数据
     * 来源，端口实现内部兜底不打挂主链路。
     */
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

    /**
     * 落一步 finish trace —— 收敛轮没有执行体，所以入参与观察为空、延迟记 0、视为成功。
     * 不与工具步共用一个可空参数的落库方法：那样「无执行体」这个事实会在每个取值处各判一次空，
     * 而把结果类型收成非空，真正需要这个判断的只有调用点本身。
     */
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

    /**
     * 一轮的执行结果 — 下一轮的起始步序、本轮执行过的工具名（finish 记在最后）、是否收敛。
     *
     * @param nextStepIndex 本轮之后下一个要落的步序（步序跨轮连续，1 起）
     * @param toolPath      本轮执行过的工具名；finish 轮也计入，让「模型选了什么」在整条路径上是完整的一段
     */
    private record RoundResult(int nextStepIndex, List<String> toolPath, boolean finished) {}

    /** 工具执行结果 — succeeded=false 时 observation 即失败原因（模型据此重试或收敛）；成功步 errorMsg 为 null（trace 不落）。 */
    private record ToolResult(boolean succeeded, String observation) {

        @Nullable
        String errorMsg() {
            return succeeded ? null : observation;
        }
    }

    /** 工具面（一次请求内） — 两种框架形态（schema 下发的回调列表、按名执行的回调表）绑在一处并按名分发；召回累加器归 {@link ChatTools} 实例，不进这里。 */
    private record ToolDispatcher(List<ToolCallback> callbacks, Map<String, ToolCallback> byName) {

        static ToolDispatcher of(ChatTools tools) {
            List<ToolCallback> callbacks = List.of(ToolCallbacks.from(tools));
            var byName = callbacks.stream()
                    .collect(Collectors.toMap(
                            callback -> callback.getToolDefinition().name(), Function.identity()));
            return new ToolDispatcher(callbacks, byName);
        }

        /** 按名称分发执行 — 未知工具与执行异常（参数不合 schema / 工具内部故障）都收敛成失败观察：模型据此重试或收敛，不把整轮对话打死。 */
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
                String reason = failureReason(e.getCause() != null ? e.getCause() : e);
                log.warn("action=tool_call_failed, tool={}, input={}, reason={}", tool, toolInputOf(decision), reason);
                return new ToolResult(false, reason);
            }
        }
    }

    /** 画像归属用户 — 机器主体返回 null（长期记忆不落库），与 trace 的 user_id 口径一致。 */
    @Nullable
    private static String attributedUserId(Input input) {
        return MACHINE_SUBJECT.equals(input.userId()) ? null : input.userId();
    }

    /** 首条 user 消息（问题 / 历史 / 画像）— 每请求固定不变，是全部轮次共享的前缀：改一个字节这轮的 KV cache 就全部作废。 */
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
                """.formatted(input.question(), formatHistory(input.history()), UserPreference.format(input.prefs()));
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
     * 工具入参摘要（trace 落库与失败日志用）—— 按工具名取对应分量，其余工具的分量为 null 是常态。
     * 工具名缺失时已在 {@link ToolCallDecision} 构造期收敛成空串，这里走 default 分支即可 ——
     * 不必再判空，否则等于让同一件事在两层各防一次。
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

    private static String failureReason(Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
