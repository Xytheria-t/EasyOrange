package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.retrieval.AssetSourcingService;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalService;
import com.cartethyia.easyorange.ai.application.support.AiModelRouter;
import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.application.support.ChatBudgetGuard;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
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
import com.cartethyia.easyorange.ai.domain.port.UserPreferenceRepository;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 多步 Agent 工具循环（ReAct）— 逐轮「决策 → 工具 → 观察」推进，直到模型判定信息足够（finish）。
 * <p>
 * 手写循环 + 原生 tool calling：每轮把 7 个工具的 JSON Schema（{@link AgentTools} 的 {@code @Tool}
 * 注解生成）随请求下发，模型返回「调哪个工具 + 参数 + 理由」。调不调、调几次、调什么由本类决定
 * —— Spring AI 2.0 的 {@code ChatModel.call} 不自动执行工具，步数与降级的循环控制权留在本类
 * （预算余量问 {@link ChatBudgetGuard}，本类只决定何时因它切断）。
 * <p>
 * 降级口径（自治循环被切断，退回确定性单次生成，已积累的观察不丢弃）：步数超限 / 预算超限
 * （与入口预检同一判定，见 {@link ChatBudgetGuard}）→ 用已积累观察直接生成；决策失败 → 按原始问题补一次
 * 检索后直接生成。工具执行失败不算决策失败：收敛成失败观察交回模型修复，不打断对话。
 * <p>
 * 每轮 trace 落库、每步推 SSE step 事件、每次循环计指标 —— 三者都是观测副产物，失败绝不影响主链路。
 * 指标落在 {@link AgentLoopMetrics}、预算判定落在 {@link ChatBudgetGuard}：本类只管「决策 → 工具 → 观察」，
 * 不知道指标名与 tag 契约，也不管这次调用还发不发得出去。
 */
@Slf4j
@Component
public class AgentLoopRunner {

    private static final String TOOL_PROMPT = "ai_chat_tool_system";
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

    /**
     * 机器主体的统一标识 —— 唯一非登录调用方是评估跑批（{@code GoldenSetEvaluator} 定时回归 / CI 门禁）：
     * 会话 / 缓存键照常按此主体隔离，但画像不落库（{@code AgentTools} 拒写）、trace 的 user_id 为空
     * （{@link #subjectUserId} 收敛）。对话的 HTTP 入口不存在匿名路径 —— 身份缺失即 401，
     * 所以「机器主体」不等于「匿名用户」。哨兵值定义在本类（唯一判定画像是否落库的地方），
     * 调用方只负责把该主体传进来。
     */
    public static final String MACHINE_SUBJECT = "machine";

    private final AiModelSupport aiModelSupport;
    private final AiModelRouter modelRouter;
    private final PromptRegistry promptRegistry;
    private final KnowledgeRetrievalService retrievalService;
    private final AssetSourcingService assetSourcingService;
    private final AssetDetailPort assetDetailPort;
    private final AgentTracePort tracePort;
    private final UserPreferenceRepository preferenceRepository;
    private final ChatBudgetGuard budgetGuard;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final IdGenerator idGenerator;
    private final AgentLoopMetrics metrics;

    public AgentLoopRunner(
            AiModelSupport aiModelSupport,
            AiModelRouter modelRouter,
            PromptRegistry promptRegistry,
            KnowledgeRetrievalService retrievalService,
            AssetSourcingService assetSourcingService,
            AssetDetailPort assetDetailPort,
            AgentTracePort tracePort,
            UserPreferenceRepository preferenceRepository,
            ChatBudgetGuard budgetGuard,
            AiProperties aiProperties,
            ObjectMapper objectMapper,
            IdGenerator idGenerator,
            AgentLoopMetrics metrics) {
        this.aiModelSupport = aiModelSupport;
        this.modelRouter = modelRouter;
        this.promptRegistry = promptRegistry;
        this.retrievalService = retrievalService;
        this.assetSourcingService = assetSourcingService;
        this.assetDetailPort = assetDetailPort;
        this.tracePort = tracePort;
        this.preferenceRepository = preferenceRepository;
        this.budgetGuard = budgetGuard;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.metrics = metrics;
    }

    /**
     * 一次循环的输入 — 记忆（历史 / 画像）由调用方装配，循环只管「决策 → 工具 → 观察」。
     *
     * @param userId  登录用户 ID；评估跑批（非登录调用方）传 {@link #MACHINE_SUBJECT}，画像不落库
     * @param handler 流式回调，可空：非流式路径不推 step 事件，trace / 指标照常
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
     * rounds 含 finish 轮，决策失败轮不计（那一轮没有决策）。
     */
    public record Result(
            List<KnowledgeHit> knowledgeHits,
            List<AssetHit> assets,
            List<AssetDetail> details,
            LoopOutcome outcome,
            int rounds) {}

    public Result run(Input input) {
        try {
            Result result = executeLoop(input);
            metrics.recordLoop(result.outcome(), result.rounds());
            return result;
        } catch (RuntimeException e) {
            metrics.recordLoop(LoopOutcome.ERROR, 0);
            throw e;
        }
    }

    private Result executeLoop(Input input) {
        String traceId = idGenerator.generateId();
        var tools = agentToolsFor(input);
        var toolFace = ToolFace.of(tools);
        var conversation = new DecisionConversation(promptRegistry.require(TOOL_PROMPT), baseStepUserMessage(input));
        int rounds = 0;

        for (int round = 1; round <= aiProperties.chat().maxSteps(); round++) {
            if (round > 1 && budgetGuard.exhausted()) {
                log.warn(
                        "action=agent_loop_degraded, reason=budget, sessionId={}, rounds={}",
                        input.sessionId(),
                        rounds);
                return snapshot(tools, LoopOutcome.BUDGET, rounds);
            }
            Optional<StepDecision> decided = decideStep(input, conversation.snapshot(), toolFace.callbacks());
            if (decided.isEmpty()) {
                // 识别不出检索需求时仍补一次：最坏是多几条不相关片段，好过把检索链路失效伪装成「无需检索」
                try {
                    tools.recallKnowledgeFallback(input.question());
                } catch (Exception e) {
                    log.warn(
                            "action=agent_fallback_search_failed, sessionId={}, reason={}",
                            input.sessionId(),
                            reasonOf(e));
                }
                return snapshot(tools, LoopOutcome.DECISION_FAILED, rounds);
            }
            rounds = round;
            StepDecision step = decided.get();

            if (AgentTools.TOOL_FINISH.equals(step.decision().tool())) {
                recordStep(input, traceId, round, step.decision(), null, null, 0);
                return snapshot(tools, LoopOutcome.FINISHED, rounds);
            }
            executeToolStep(input, traceId, round, step, toolFace, conversation);
        }
        return snapshot(tools, LoopOutcome.STEP_LIMIT, rounds);
    }

    /** 按请求装配工具实例 — 召回累加器随实例隔离（所有权在 {@link AgentTools}），出口经只读快照收取。 */
    private AgentTools agentToolsFor(Input input) {
        return new AgentTools(
                retrievalService, assetSourcingService, assetDetailPort, preferenceRepository, subjectUserId(input));
    }

    private static Result snapshot(AgentTools tools, LoopOutcome outcome, int rounds) {
        return new Result(tools.knowledgeHits(), tools.assets(), tools.details(), outcome, rounds);
    }

    /**
     * 一步决策：工具 schema 随请求下发，模型以原生 tool calling 返回「调用哪个工具 + 参数」。
     * 决策失败（调用故障 / 未返回工具调用 / 参数 JSON 不可解析）返回 empty，由调用方走单步降级
     * —— 循环内不重试，一次请求最多一次决策故障。
     */
    private Optional<StepDecision> decideStep(
            Input input, List<Message> decisionMessages, List<ToolCallback> toolCallbacks) {
        try {
            List<AssistantMessage.ToolCall> toolCalls = aiModelSupport.callWithTools(
                    modelRouter.choose("chat_tool"), AiCallScope.CHAT, decisionMessages, toolCallbacks);
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
            return Optional.of(
                    new StepDecision(decision.withToolCall(toolCall.name(), toolCall.arguments()), toolCall));
        } catch (Exception e) {
            log.warn(
                    "action=agent_decision_failed, fallback=single_step, sessionId={}, reason={}",
                    input.sessionId(),
                    reasonOf(e));
            return Optional.empty();
        }
    }

    /** 执行一步工具并落成观测副产物（trace 落库 / SSE step 事件 / 步级指标），再按对话协议回填决策消息序列。 */
    private void executeToolStep(
            Input input,
            String traceId,
            int round,
            StepDecision step,
            ToolFace toolFace,
            DecisionConversation conversation) {
        AgentStepDecision decision = step.decision();
        long start = System.nanoTime();
        ToolOutcome outcome = toolFace.invoke(decision);
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        metrics.recordTool(decision.tool(), latencyMs);

        recordStep(input, traceId, round, decision, toolInputOf(decision), outcome, latencyMs);
        conversation.appendStep(step.toolCall(), outcome.observation());
    }

    /**
     * 落一步 trace 并推 SSE step 事件 —— 前端步骤可视化与「平均步数 / 降级率 / 步级延迟」口径的数据
     * 来源，端口实现内部兜底不打挂主链路。finish 收敛轮无执行体：toolInput / outcome 均为 null。
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
                    round, decision.tool(), decision.thought(), outcome == null ? null : outcome.observation()));
        }
    }

    /** 一步决策 — 解析后的决策视图 + 原生 tool call（id / name / arguments 都从这来回填消息序列）。 */
    private record StepDecision(AgentStepDecision decision, AssistantMessage.ToolCall toolCall) {}

    /** 工具执行结果 — success=false 时 observation 即失败原因（模型据此重试或收敛）；成功步 errorMsg 为 null（trace 不落）。 */
    private record ToolOutcome(boolean success, String observation) {

        @Nullable
        String errorMsg() {
            return success ? null : observation;
        }
    }

    /** 一次请求的工具面 — {@link AgentTools} 实例与两种框架形态（schema 下发的回调列表、按名执行的回调表）绑在一处。 */
    private record ToolFace(AgentTools tools, List<ToolCallback> callbacks, Map<String, ToolCallback> byName) {

        static ToolFace of(AgentTools tools) {
            List<ToolCallback> callbacks = List.of(ToolCallbacks.from(tools));
            var byName = callbacks.stream()
                    .collect(Collectors.toMap(
                            callback -> callback.getToolDefinition().name(), Function.identity()));
            return new ToolFace(tools, callbacks, byName);
        }

        /** 按名称分发执行 — 未知工具与执行异常（参数不合 schema / 工具内部故障）都收敛成失败观察：模型据此重试或收敛，不把整轮对话打死。 */
        ToolOutcome invoke(AgentStepDecision decision) {
            String tool = decision.tool() == null ? "" : decision.tool();
            ToolCallback callback = byName.get(tool);
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
    }

    /** 画像归属用户 — 机器主体返回 null（长期记忆不落库），与 trace 的 user_id 口径一致。 */
    private static String subjectUserId(Input input) {
        return MACHINE_SUBJECT.equals(input.userId()) ? null : input.userId();
    }

    /** 首条 user 消息（问题 / 历史 / 画像）— 每请求固定不变，是全部轮次共享的前缀：改一个字节这轮的 KV cache 就全部作废。 */
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

    /** 工具入参摘要（trace 落库与失败日志用）。 */
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
