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
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 多步 Agent 工具循环（ReAct）— 逐轮「决策 → 工具 → 观察」推进，直到模型判定信息足够（finish）。
 * <p>
 * 编排结构（自治循环：调不调、调几次、调什么参数都由模型逐轮决定）：
 * 每轮把 7 个工具的 JSON Schema（{@link AgentTools} 的 {@code @Tool} 注解生成、供应商侧校验）随请求发出，
 * 模型以原生 tool calling 返回「调用哪个工具 + 参数 + 理由」；工具执行结果按对话协议原样回填进决策
 * 消息序列（assistant 的 tool_call 消息 + role=tool 的观察消息），逐轮累积 —— 首两条（system +
 * 首条 user）固定不变，轮间前缀稳定命中供应商 KV cache 折扣，与生成路径同一成本口径。
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
 *       伪装成「无需检索」）；补检索自身故障不再外抛，以已有召回物继续生成，不让对话死在降级路径上。</li>
 * </ul>
 * 工具执行失败（参数不合 schema / 工具内部故障）不算决策失败：收敛成失败观察交回模型（带错误反馈的
 * 修复轮），与「查无此资产」同属正常观察，不打断对话。
 * 每轮 trace 落库（{@link AgentTracePort}）、每步向流式回调推 step 事件（前端步骤可视化）、
 * 每次循环计指标（步数分布 / 步级延迟 / 循环结局）—— 三者都是观测副产物，失败绝不影响主链路。
 */
@Slf4j
@Component
public class AgentLoopRunner {

    static final String OUTCOME_FINISHED = "finished";
    static final String OUTCOME_STEP_LIMIT = "step_limit";
    static final String OUTCOME_BUDGET = "budget";
    static final String OUTCOME_DECISION_FAILED = "decision_failed";
    /** 基础设施故障穿透的哨兵结局（正常应为零）：决策 / 工具失败都已在循环内收敛成降级。 */
    static final String OUTCOME_ERROR = "error";

    private static final String TOOL_PROMPT = "ai_chat_tool_system";
    private static final String LOOP_METRIC = "easyorange.ai.chat.loop";
    private static final String TOOL_METRIC = "easyorange.ai.chat.tool";
    private static final String STEP_DURATION_METRIC = "easyorange.ai.chat.step.duration";
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

    /** 循环结局计数 —— 结局集合封闭（四条出口 + error 哨兵），构造期一次注册，热路径零查找。 */
    private final Map<String, Counter> loopCounters;

    /** 每请求决策轮数分布 —— 口径同样封闭，构造期注册（避免每请求 builder 分配）。 */
    private final DistributionSummary stepsSummary;

    public AgentLoopRunner(
            AiModelSupport aiModelSupport,
            AiModelRouter modelRouter,
            PromptRegistry promptRegistry,
            KnowledgeRetrievalService retrievalService,
            AssetSourcingService assetSourcingService,
            AssetDetailPort assetDetailPort,
            AgentTracePort tracePort,
            UserPreferenceRepository preferenceRepository,
            TokenBudgetStore budgetStore,
            AiProperties aiProperties,
            ObjectMapper objectMapper,
            IdGenerator idGenerator,
            MeterRegistry meterRegistry) {
        this.aiModelSupport = aiModelSupport;
        this.modelRouter = modelRouter;
        this.promptRegistry = promptRegistry;
        this.retrievalService = retrievalService;
        this.assetSourcingService = assetSourcingService;
        this.assetDetailPort = assetDetailPort;
        this.tracePort = tracePort;
        this.preferenceRepository = preferenceRepository;
        this.budgetStore = budgetStore;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.meterRegistry = meterRegistry;
        this.loopCounters = Map.of(
                OUTCOME_FINISHED,
                meterRegistry.counter(LOOP_METRIC, "outcome", OUTCOME_FINISHED),
                OUTCOME_STEP_LIMIT,
                meterRegistry.counter(LOOP_METRIC, "outcome", OUTCOME_STEP_LIMIT),
                OUTCOME_BUDGET,
                meterRegistry.counter(LOOP_METRIC, "outcome", OUTCOME_BUDGET),
                OUTCOME_DECISION_FAILED,
                meterRegistry.counter(LOOP_METRIC, "outcome", OUTCOME_DECISION_FAILED),
                OUTCOME_ERROR,
                meterRegistry.counter(LOOP_METRIC, "outcome", OUTCOME_ERROR));
        this.stepsSummary = DistributionSummary.builder("easyorange.ai.chat.steps")
                .description("每次对话请求的 Agent 决策轮数（含 finish 轮）")
                .publishPercentiles(0.5, 0.95)
                .register(meterRegistry);
    }

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
     * @param outcome       finished / step_limit / budget / decision_failed / error
     * @param rounds        已完成的决策轮数（含 finish 轮；决策失败轮不计，那一轮没有决策）
     */
    public record Result(
            List<KnowledgeHit> knowledgeHits,
            List<AssetHit> assets,
            List<AssetDetail> details,
            String outcome,
            int rounds) {}

    /** 一步决策 — 解析后的决策视图 + 原生 tool call（回填消息序列用：id / name / arguments 都从这来）。 */
    private record StepDecision(AgentStepDecision decision, AssistantMessage.ToolCall toolCall) {}

    /** 工具执行结果 — success=false 时 observation 即失败原因（模型据此决定重试或收敛）。 */
    private record ToolOutcome(boolean success, String observation) {

        /** 失败步的错误原因与交回模型的那段观察文本同源；成功步为 null（trace 不落 errorMsg）。 */
        @Nullable
        String errorMsg() {
            return success ? null : observation;
        }
    }

    public Result run(Input input) {
        try {
            Result result = executeLoop(input);
            loopCounters.get(result.outcome()).increment();
            stepsSummary.record(result.rounds());
            return result;
        } catch (RuntimeException e) {
            loopCounters.get(OUTCOME_ERROR).increment();
            throw e;
        }
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
        // 决策消息按对话协议逐轮累积：[system, user(问题/历史/画像), assistant(tool_call), tool(观察), …]
        // 首两条每请求固定，轮间前缀稳定 → 供应商 KV cache 折扣可命中（与生成路径同一成本口径）
        List<Message> decisionMessages = new ArrayList<>();
        decisionMessages.add(new SystemMessage(promptRegistry.require(TOOL_PROMPT)));
        decisionMessages.add(new UserMessage(baseStepUserMessage(input)));
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
            Optional<StepDecision> decided = decideStep(input, decisionMessages, toolCallbacks);
            if (decided.isEmpty()) {
                // 决策失败降级：按原始问题补一次知识库检索（规则类问题走检索是常态，
                // 识别不出来最坏是多几条不相关片段进 prompt，好过把检索链路失效伪装成「无需检索」）。
                // 补检索自身故障不外抛：以已有召回物继续生成（与工具步「查不到就如实说」同语义），
                // 不让对话死在降级路径上
                try {
                    hits.addAll(retrievalService.search(input.question(), AgentTools.RETRIEVAL_TOP_K));
                } catch (Exception e) {
                    log.warn(
                            "action=agent_fallback_search_failed, sessionId={}, reason={}",
                            input.sessionId(),
                            reasonOf(e));
                }
                return snapshot(hits, assets, details, OUTCOME_DECISION_FAILED, rounds);
            }
            rounds = round;
            StepDecision step = decided.get();

            if (AgentTools.TOOL_FINISH.equals(step.decision().tool())) {
                recordStep(input, traceId, round, step.decision(), null, null, 0);
                return snapshot(hits, assets, details, OUTCOME_FINISHED, rounds);
            }
            executeToolStep(input, traceId, round, step, callbacksByName, decisionMessages);
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
    private Optional<StepDecision> decideStep(
            Input input, List<Message> decisionMessages, List<ToolCallback> toolCallbacks) {
        try {
            List<AssistantMessage.ToolCall> toolCalls = aiModelSupport.callWithTools(
                    modelRouter.choose("chat_tool"),
                    AiCallScope.CHAT,
                    // 快照而非可变引用：供应商调用收到当轮的不可变序列，调用后循环继续追加互不可见
                    List.copyOf(decisionMessages),
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
            return Optional.of(new StepDecision(decision.withToolCall(toolCall.name(), toolCall.arguments()), toolCall));
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
     * （流式回调）、步级指标（调用计数 + 耗时）；再按对话协议把本步回填进决策消息序列
     * （assistant tool_call + role=tool 观察，失败观察原样回填，模型据此修复）。
     */
    private void executeToolStep(
            Input input,
            String traceId,
            int round,
            StepDecision step,
            Map<String, ToolCallback> callbacksByName,
            List<Message> decisionMessages) {
        AgentStepDecision decision = step.decision();
        toolCounter(decision.tool()).increment();

        long start = System.nanoTime();
        ToolOutcome outcome = invokeTool(decision, callbacksByName);
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        stepTimer(decision.tool()).record(latencyMs, TimeUnit.MILLISECONDS);

        String toolInput = toolInputOf(decision);
        recordStep(input, traceId, round, decision, toolInput, outcome, latencyMs);
        decisionMessages.add(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(step.toolCall()))
                .build());
        decisionMessages.add(ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse(
                        step.toolCall().id(), step.toolCall().name(), outcome.observation())))
                .build());
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

    /**
     * 落一步 trace 并向流式回调推 step 事件 —— 前端步骤可视化与「平均步数 / 降级率 / 步级延迟」
     * 三个口径的数据来源，两处都是观测副产物（端口实现内部兜底，不打挂主链路）。
     * <p>
     * finish 收敛轮无执行体：toolInput / outcome 均为 null（trace 记 success、零耗时、无观察）。
     */
    private void recordStep(
            Input input,
            String traceId,
            int round,
            AgentStepDecision decision,
            @Nullable String toolInput,
            @Nullable ToolOutcome outcome,
            long latencyMs) {
        tracePort.record(new AgentStepTrace(
                traceId,
                input.sessionId(),
                subjectUserId(input),
                round,
                decision.tool(),
                toolInput,
                decision.thought(),
                outcome == null ? null : outcome.observation(),
                latencyMs,
                outcome == null || outcome.success(),
                outcome == null ? null : outcome.errorMsg()));
        ChatStreamHandler handler = input.handler();
        if (handler != null) {
            handler.onStep(new AgentStepView(
                    round,
                    decision.tool(),
                    decision.thought(),
                    outcome == null ? null : outcome.observation()));
        }
    }

    private Counter toolCounter(String tool) {
        return meterRegistry.counter(TOOL_METRIC, "name", tool);
    }

    private Timer stepTimer(String tool) {
        return Timer.builder(STEP_DURATION_METRIC)
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

    /**
     * 首条 user 消息 — 问题 / 历史 / 画像，每请求固定不变（观察不拼在这里，而是按协议以 role=tool
     * 消息逐轮回填）：它是全部轮次共享的前缀，改一个字节这轮的 KV cache 就全部作废。
     */
    private static String baseStepUserMessage(Input input) {
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

    /** 工具入参摘要（trace 落库与失败日志用）—— 每个工具取自有字段，其余轮次即检索词。 */
    private static String toolInputOf(AgentStepDecision decision) {
        return switch (decision.tool() == null ? "" : decision.tool()) {
            case AgentTools.TOOL_PRODUCT_DETAIL -> decision.productId();
            case AgentTools.TOOL_COMPARE_ASSETS ->
                decision.productIds() == null ? null : String.join("、", decision.productIds());
            case AgentTools.TOOL_REMEMBER_PREFERENCE ->
                orEmpty(decision.preferenceKey()) + "=" + orEmpty(decision.preferenceValue());
            default -> decision.query();
        };
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static String reasonOf(Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
