package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.retrieval.AssetSourcingService;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalService;
import com.cartethyia.easyorange.ai.application.support.AiModelRouter;
import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.model.AgentStepDecision;
import com.cartethyia.easyorange.ai.domain.model.AgentStepTrace;
import com.cartethyia.easyorange.ai.domain.model.AgentStepView;
import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.UserPreference;
import com.cartethyia.easyorange.ai.domain.port.AgentTracePort;
import com.cartethyia.easyorange.ai.domain.port.AssetDetailPort;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStore;
import com.cartethyia.easyorange.ai.domain.port.UserPreferenceRepository;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 多步 Agent 工具循环（ReAct）— 逐轮「决策 → 工具 → 观察」推进，直到模型判定信息足够（finish）。
 * <p>
 * 编排结构（自治循环：调不调、调几次、调什么参数都由模型逐轮决定）：
 * 每轮把 7 个工具的 JSON Schema（{@link AgentTools} 的 {@code @Tool} 注解生成、供应商侧校验）随请求发出，
 * 模型以原生 tool calling 返回「调用哪个工具 + 参数 + 理由」，工具执行结果作为观察进入下一轮决策上下文；
 * 规则类与找货类需求兼有时由模型分两步分别检索，而非一次穷举。
 * <b>手写循环 + 原生模型接口</b>：工具只负责 schema 与「执行 + 观察格式」，调不调、调几次由本类决定
 * —— Spring AI 2.0 的 {@code ChatModel.call} 不自动执行工具（自动执行已收进 ChatClient 的
 * ToolCallingAdvisor），步数 / 预算 / 降级这些循环控制权都留在本类。
 * <p>
 * 降级口径（自治循环被切断，退回确定性的单次生成，已积累的观察不丢弃）：
 * <ul>
 *   <li><b>步数超限</b> — 上限内未 finish：不再发第 N+1 次决策调用，直接用已积累观察生成；</li>
 *   <li><b>预算超限</b> — 循环中途日预算余量不足（与流式入口 {@link #chatBudgetExhausted} 同一判定）：
 *       同上强制生成，最坏超发被 maxTokensPerCall 兜住；</li>
 *   <li><b>决策失败</b> — 决策调用故障 / 未返回工具调用 / 参数 JSON 不可解析：按原始问题补一次知识库检索后
 *       直接生成（规则类问题走检索是常态，识别不出来最坏是多几条不相关片段进 prompt，好过把检索链路失效
 *       伪装成「无需检索」）。</li>
 * </ul>
 * 工具执行失败（参数不合 schema / 工具内部故障）不算决策失败：收敛成失败观察交回模型（带错误反馈的
 * 修复轮），与「查无此资产」同属正常观察，不打断对话。
 * 每轮 trace 落库（{@link AgentTracePort}）、每步向流式回调推 step 事件（前端步骤可视化）、
 * 每次循环计指标（步数分布 / 步级延迟 / 循环结局）—— 三者都是观测副产物，失败绝不影响主链路。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentLoopRunner {

    static final String OUTCOME_FINISHED = "finished";
    static final String OUTCOME_STEP_LIMIT = "step_limit";
    static final String OUTCOME_BUDGET = "budget";
    static final String OUTCOME_DECISION_FAILED = "decision_failed";

    private static final String TOOL_PROMPT = "ai_chat_tool_system";
    /** 预算场景键取自 {@link AiCallScope}（枚举名小写），与 {@code @TokenBudget(scenario=...)} 单点同源不重写。 */
    private static final String CHAT_SCENARIO = AiCallScope.CHAT.budgetScenario();
    /** 未知工具观察里的工具清单（与 {@link AgentTools} 的常量同源，不重写字面量）。 */
    private static final String TOOL_MENU = String.join(
            " / ",
            AgentTools.TOOL_KNOWLEDGE_SEARCH,
            AgentTools.TOOL_PRODUCT_SEARCH,
            AgentTools.TOOL_PRODUCT_DETAIL,
            AgentTools.TOOL_MARKET_PRICE_STATS,
            AgentTools.TOOL_COMPARE_ASSETS,
            AgentTools.TOOL_REMEMBER_PREFERENCE,
            AgentTools.TOOL_FINISH);

    /** 与 {@code @TokenBudget(scenario="chat")} 注解默认值一致（yaml 缺失时兜底；改注解要同步改这里）。 */
    private static final int DEFAULT_MAX_TOKENS_PER_CALL = 1500;

    private static final int DEFAULT_DAILY_LIMIT = 300_000;

    /** 匿名会话的用户标识 —— {@link Input#userId()} 的契约值，入口 {@code AiChatService} 与本类共用这一份。 */
    static final String ANONYMOUS_USER = "anonymous";

    private final AiModelSupport aiModelSupport;
    private final AiModelRouter modelRouter;
    private final PromptRegistry promptRegistry;
    private final KnowledgeRetrievalService retrievalService;
    private final AssetSourcingService assetSourcingService;
    private final AssetDetailPort assetDetailPort;
    private final AgentTracePort tracePort;
    private final UserPreferenceRepository preferenceRepository;
    private final TokenBudgetStore budgetStore;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final IdGenerator idGenerator;
    private final MeterRegistry meterRegistry;

    /**
     * 一次循环的输入 — 记忆（历史 / 画像）由调用方装配，循环只管「决策 → 工具 → 观察」。
     *
     * @param question  用户问题
     * @param sessionId 会话 ID（trace 归属）
     * @param userId    用户 ID（匿名时为 "anonymous"，画像不落库）
     * @param history   会话历史（决策上下文）
     * @param prefs     用户画像（决策上下文）
     * @param handler   流式回调（可空：非流式路径不推 step 事件，trace / 指标照常）
     */
    public record Input(
            String question,
            String sessionId,
            String userId,
            List<ChatTurn> history,
            List<UserPreference> prefs,
            @Nullable ChatStreamHandler handler) {}

    /**
     * 循环结果 — 召回物供最终生成装配 prompt 与引用溯源；outcome / rounds 供指标与降级归因。
     *
     * @param knowledgeHits 全部轮次累加的知识库命中
     * @param assets        全部轮次累加的在售资产命中
     * @param details       product_detail 查得的资产详情
     * @param outcome       finished / step_limit / budget / decision_failed
     * @param rounds        已完成的决策轮数（含 finish 轮；决策失败轮不计，那一轮没有决策）
     */
    public record Result(
            List<KnowledgeHit> knowledgeHits,
            List<AssetHit> assets,
            List<AssetDetail> details,
            String outcome,
            int rounds) {}

    /** 单步观察 — 进下一轮决策上下文的「已执行步骤」记录。 */
    private record StepObservation(String tool, String input, String observation) {}

    /** 工具执行结果 — success=false 时 observation 即失败原因（模型据此决定重试或收敛）。 */
    private record ToolOutcome(boolean success, String observation) {

        /** 失败步的错误原因与交回模型的那段观察文本同源；成功步为 null（trace 不落 errorMsg）。 */
        @Nullable
        String errorMsg() {
            return success ? null : observation;
        }
    }

    public Result run(Input input) {
        Result result = executeLoop(input);
        loopCounter(result.outcome()).increment();
        stepsSummary().record(result.rounds());
        return result;
    }

    /**
     * chat 场景日预算前置检查 — 流式入口（AiChatService.checkBudget）与循环中途共用同一判定，
     * 判据单处维护两处生效（used + maxPerCall > dailyLimit，与 TokenBudgetAspect 同式）。
     */
    public boolean chatBudgetExhausted() {
        int used = budgetStore
                .getTodayUsage(CHAT_SCENARIO)
                .map(TokenBudgetStore.TokenUsage::total)
                .orElse(0);
        var cfg = aiProperties.budget().resolve(CHAT_SCENARIO);
        int maxPerCall = cfg != null ? cfg.maxTokensPerCall() : DEFAULT_MAX_TOKENS_PER_CALL;
        int dailyLimit = cfg != null ? cfg.dailyTokenLimit() : DEFAULT_DAILY_LIMIT;
        return dailyLimit > 0 && used + maxPerCall > dailyLimit;
    }

    private Result executeLoop(Input input) {
        String traceId = idGenerator.generateId();
        List<KnowledgeHit> hits = new ArrayList<>();
        List<AssetHit> assets = new ArrayList<>();
        List<AssetDetail> details = new ArrayList<>();
        List<StepObservation> observations = new ArrayList<>();
        // 工具实例与回调表按请求构建一次：召回物累加器跨轮复用，工具 schema 每轮随决策调用发出
        var tools = new AgentTools(
                hits,
                assets,
                details,
                retrievalService,
                assetSourcingService,
                assetDetailPort,
                preferenceRepository,
                subjectUserId(input));
        List<ToolCallback> toolCallbacks = List.of(ToolCallbacks.from(tools));
        Map<String, ToolCallback> callbacksByName = toolCallbacks.stream()
                .collect(Collectors.toMap(
                        callback -> callback.getToolDefinition().name(), Function.identity()));
        int maxSteps = aiProperties.chat().maxSteps();
        int rounds = 0;

        for (int round = 1; round <= maxSteps; round++) {
            if (round > 1 && chatBudgetExhausted()) {
                log.warn(
                        "action=agent_loop_degraded, reason=budget, sessionId={}, rounds={}",
                        input.sessionId(),
                        rounds);
                return snapshot(hits, assets, details, OUTCOME_BUDGET, rounds);
            }
            Optional<AgentStepDecision> decided = decideStep(input, observations, toolCallbacks);
            if (decided.isEmpty()) {
                // 决策失败降级：按原始问题补一次知识库检索（规则类问题走检索是常态，
                // 识别不出来最坏是多几条不相关片段进 prompt，好过把检索链路失效伪装成「无需检索」）
                hits.addAll(retrievalService.search(input.question(), AgentTools.RETRIEVAL_TOP_K));
                return snapshot(hits, assets, details, OUTCOME_DECISION_FAILED, rounds);
            }
            rounds = round;
            AgentStepDecision decision = decided.get();

            if (AgentTools.TOOL_FINISH.equals(decision.tool())) {
                recordFinishStep(input, traceId, round, decision);
                return snapshot(hits, assets, details, OUTCOME_FINISHED, rounds);
            }
            executeToolStep(input, traceId, round, decision, callbacksByName, observations);
        }
        return snapshot(hits, assets, details, OUTCOME_STEP_LIMIT, rounds);
    }

    /**
     * 退出快照 — 四条出口（finish / 步数超限 / 预算耗尽 / 决策失败）共用同一口径：召回物拷贝成不可变
     * （循环内的累加器仍被工具实例持有，不把可变引用交出去），outcome 与轮数供指标与降级归因。
     */
    private static Result snapshot(
            List<KnowledgeHit> hits, List<AssetHit> assets, List<AssetDetail> details, String outcome, int rounds) {
        return new Result(List.copyOf(hits), List.copyOf(assets), List.copyOf(details), outcome, rounds);
    }

    /**
     * 步骤决策：工具 schema 随请求下发，模型以原生 tool calling 返回「调用哪个工具 + 参数」。
     * 决策失败（调用故障 / 未返回工具调用 / 参数 JSON 不可解析）返回 empty，由调用方走单步降级
     * —— 不在循环里重试，一次请求最多一次决策故障。
     */
    private Optional<AgentStepDecision> decideStep(
            Input input, List<StepObservation> observations, List<ToolCallback> toolCallbacks) {
        try {
            List<AssistantMessage.ToolCall> toolCalls = aiModelSupport.callWithTools(
                    modelRouter.choose("chat_tool"),
                    AiCallScope.CHAT,
                    promptRegistry.require(TOOL_PROMPT),
                    buildStepUserMessage(input, observations),
                    toolCallbacks);
            if (toolCalls.isEmpty()) {
                log.warn(
                        "action=agent_decision_failed, fallback=single_step, sessionId={}, reason=模型未返回工具调用",
                        input.sessionId());
                return Optional.empty();
            }
            if (toolCalls.size() > 1) {
                // 循环按「每步一个工具」推进：并行工具调用只取第一个，其余留给下一轮（模型可再发起）
                log.info(
                        "action=agent_parallel_tool_calls, sessionId={}, count={}",
                        input.sessionId(),
                        toolCalls.size());
            }
            AssistantMessage.ToolCall toolCall = toolCalls.getFirst();
            AgentStepDecision decision = objectMapper.readValue(toolCall.arguments(), AgentStepDecision.class);
            return Optional.of(decision.withToolCall(toolCall.name(), toolCall.arguments()));
        } catch (Exception e) {
            log.warn(
                    "action=agent_decision_failed, fallback=single_step, sessionId={}, reason={}",
                    input.sessionId(),
                    reasonOf(e));
            return Optional.empty();
        }
    }

    /**
     * 执行一步工具，并把该步落成观测副产物：trace 落库（{@link AgentTracePort}）、SSE step 事件
     * （流式回调）、步级指标（调用计数 + 耗时）；观察文本追加进下一步决策上下文。
     */
    private void executeToolStep(
            Input input,
            String traceId,
            int round,
            AgentStepDecision decision,
            Map<String, ToolCallback> callbacksByName,
            List<StepObservation> observations) {
        toolCounter(decision.tool()).increment();

        long start = System.nanoTime();
        ToolOutcome outcome = invokeTool(decision, callbacksByName);
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        stepTimer(decision.tool()).record(latencyMs, TimeUnit.MILLISECONDS);

        String toolInput = toolInputOf(decision);
        recordStep(input, traceId, round, decision, toolInput, outcome, latencyMs);
        observations.add(new StepObservation(decision.tool(), toolInput, outcome.observation()));
    }

    /**
     * 按名称分发到 {@link AgentTools} 的 {@code @Tool} 回调（工具语义不在循环里实现）。
     * 未知工具与工具抛异常（参数不合 schema / 工具内部故障）都收敛成失败观察：模型据此换参数重试或收敛，
     * 不把整轮对话打成不可用。
     */
    private ToolOutcome invokeTool(AgentStepDecision decision, Map<String, ToolCallback> callbacksByName) {
        String tool = decision.tool() == null ? "" : decision.tool();
        ToolCallback callback = callbacksByName.get(tool);
        if (callback == null) {
            return new ToolOutcome(false, "未知工具 %s，请改用 %s".formatted(tool, TOOL_MENU));
        }
        try {
            return new ToolOutcome(true, callback.call(decision.arguments()));
        } catch (Exception e) {
            // MethodToolCallback 把「参数转换失败」与「方法体异常」统一包成 ToolExecutionException
            String reason = reasonOf(e.getCause() != null ? e.getCause() : e);
            log.warn("action=agent_tool_failed, tool={}, input={}, reason={}", tool, toolInputOf(decision), reason);
            return new ToolOutcome(false, reason);
        }
    }

    /** 收敛轮落 trace —— 该轮无执行体：无入参 / 无观察 / 零耗时 / 无错误。 */
    private void recordFinishStep(Input input, String traceId, int round, AgentStepDecision decision) {
        recordStep(input, traceId, round, decision, null, null, 0, true, null);
    }

    /** 工具步落 trace —— 观察与成败取自 {@link ToolOutcome}（失败步的观察即错误原因）。 */
    private void recordStep(
            Input input,
            String traceId,
            int round,
            AgentStepDecision decision,
            @Nullable String toolInput,
            ToolOutcome outcome,
            long latencyMs) {
        recordStep(
                input,
                traceId,
                round,
                decision,
                toolInput,
                outcome.observation(),
                latencyMs,
                outcome.success(),
                outcome.errorMsg());
    }

    /**
     * 落一步 trace 并向流式回调推 step 事件 —— 前端步骤可视化与「平均步数 / 降级率 / 步级延迟」
     * 三个口径的数据来源，两处都是观测副产物（端口实现内部兜底，不打挂主链路）。
     */
    private void recordStep(
            Input input,
            String traceId,
            int round,
            AgentStepDecision decision,
            @Nullable String toolInput,
            @Nullable String observation,
            long latencyMs,
            boolean success,
            @Nullable String errorMsg) {
        tracePort.record(new AgentStepTrace(
                traceId,
                input.sessionId(),
                subjectUserId(input),
                round,
                decision.tool(),
                toolInput,
                decision.thought(),
                observation,
                latencyMs,
                success,
                errorMsg));
        ChatStreamHandler handler = input.handler();
        if (handler != null) {
            handler.onStep(new AgentStepView(round, decision.tool(), decision.thought(), observation));
        }
    }

    private Counter loopCounter(String outcome) {
        return meterRegistry.counter("easyorange.ai.chat.loop", "outcome", outcome);
    }

    private Counter toolCounter(String tool) {
        return meterRegistry.counter("easyorange.ai.chat.tool", "name", tool);
    }

    private DistributionSummary stepsSummary() {
        return DistributionSummary.builder("easyorange.ai.chat.steps")
                .description("每次对话请求的 Agent 决策轮数（含 finish 轮）")
                .publishPercentiles(0.5, 0.95)
                .register(meterRegistry);
    }

    private Timer stepTimer(String tool) {
        return Timer.builder("easyorange.ai.chat.step.duration")
                .tag("tool", tool)
                .publishPercentiles(0.95)
                .register(meterRegistry);
    }

    /**
     * 画像归属用户 — 匿名会话返回 null（长期记忆不落库），与 trace 的 subject 口径一致。
     * <p>
     * 偏好提取本身已移入 {@link AgentTools#rememberPreference}（独立工具、模型自主决定何时写），
     * 不再由本类按 finish 轮旁路落库——原先只在收敛轮提取，步数超限 / 预算耗尽 / 决策失败三条降级
     * 路径下偏好会静默丢失。
     */
    private static String subjectUserId(Input input) {
        return ANONYMOUS_USER.equals(input.userId()) ? null : input.userId();
    }

    private static String buildStepUserMessage(Input input, List<StepObservation> observations) {
        return """
                用户问题：
                <user_question>
                %s
                </user_question>

                历史对话：
                %s

                用户画像：
                %s

                已执行步骤：
                %s
                """.formatted(
                        input.question(),
                        formatHistory(input.history()),
                        UserPreference.format(input.prefs()),
                        formatObservations(observations));
    }

    private static String formatObservations(List<StepObservation> observations) {
        if (observations.isEmpty()) {
            return "(尚无，这是第 1 步)";
        }
        var sb = new StringBuilder();
        for (int i = 0; i < observations.size(); i++) {
            StepObservation observation = observations.get(i);
            sb.append("第 %d 步 [%s] %s\n观察：%s\n"
                    .formatted(i + 1, observation.tool(), observation.input(), observation.observation()));
        }
        return sb.toString();
    }

    private static String formatHistory(List<ChatTurn> history) {
        if (history.isEmpty()) {
            return "(无)";
        }
        return history.stream()
                .map(turn -> (turn.role().isUser() ? "用户" : "助手") + ": " + turn.content())
                .collect(Collectors.joining("\n"));
    }

    /** 工具入参摘要（trace 落库与失败日志用）—— 每个工具取自有字段，其余轮次即检索词。 */
    private static String toolInputOf(AgentStepDecision decision) {
        return switch (decision.tool() == null ? "" : decision.tool()) {
            case AgentTools.TOOL_PRODUCT_DETAIL -> decision.productId();
            case AgentTools.TOOL_COMPARE_ASSETS ->
                decision.productIds() == null ? null : String.join("、", decision.productIds());
            case AgentTools.TOOL_REMEMBER_PREFERENCE -> decision.preferenceKey() + "=" + decision.preferenceValue();
            default -> decision.query();
        };
    }

    private static String reasonOf(Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
