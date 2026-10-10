package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.support.FailureReason;
import com.cartethyia.easyorange.ai.application.support.UntrustedText;
import com.cartethyia.easyorange.ai.application.toolcall.ToolCallArguments;
import com.cartethyia.easyorange.ai.application.toolcall.ToolCallDecider;
import com.cartethyia.easyorange.ai.application.toolcall.ToolCallDecision;
import com.cartethyia.easyorange.ai.application.toolcall.ToolCallLoopOutcome;
import com.cartethyia.easyorange.ai.application.toolcall.ToolLoopDecider;
import com.cartethyia.easyorange.ai.application.toolcall.ToolLoopKernel;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.UserPreference;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistryPort;
import com.cartethyia.easyorange.ai.domain.port.ToolCallStepTracePort;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 买家侧多步工具调用循环（ReAct）— 找货对话的编排器：注入首轮上下文、工具面与降级口径，循环机制交给 {@link ToolLoopKernel}
 * （与 listing 链路共用）。
 * <p>
 * 降级口径 = 已积累观察不丢弃：步数 / 预算超限用观察直接生成；决策失败补一次检索（白名单未含检索的消融臂跳过 —— 补检索会把该臂的检索来源加回来）。
 */
@Slf4j
@Component
public class ToolCallLoop {

    /**
     * 机器主体标识 —— 唯一非登录调用方是评估跑批：会话 / 缓存键照常按此主体隔离；{@link #attributedUserId} 收敛成 null 后
     * 偏好写不进（{@link ChatTools} 拒收）、trace 的 user_id 为空。
     */
    public static final String MACHINE_SUBJECT = "machine";

    /** 决策对话的 system prompt 键（与 {@code prompts/ai_chat_tool.yml} 的 name 同名）。 */
    private static final String CHAT_TOOL_PROMPT = "ai_chat_tool_system";

    private final PromptRegistryPort promptRegistry;
    private final ChatToolsFactory toolsFactory;
    private final ToolCallDecider decider;
    private final ToolLoopKernel loopKernel;
    private final ChatBudgetGuard budgetGuard;
    private final AiProperties aiProperties;
    private final ToolCallLoopMetrics metrics;

    /** trace 端口与 trace_id 来源经内核持有（步级落库属循环机制）；签名保持内核抽取前形态，买家侧装配零改动。 */
    @Autowired
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
        this.loopKernel = new ToolLoopKernel(tracePort, idGenerator);
        this.budgetGuard = budgetGuard;
        this.aiProperties = aiProperties;
        this.metrics = metrics;
    }

    /**
     * 一次循环的输入 — 记忆（历史 / 偏好）由调用方装配，循环只管「决策 → 工具 → 观察」。
     *
     * @param sessionId 可空（请求不带会话），两处消费方各自兜底：trace 的 session_id 补 {@code anonymous} 哨兵、会话记忆 fail-open
     * @param userId 评估跑批传 {@link #MACHINE_SUBJECT}，偏好不落库
     * @param streamHandler 可空：非流式路径不推 step 事件，trace / 指标照常
     * @param allowedTools 工具白名单，null = 全量工具面（生产路径）；非空只装配列出的工具，RAG 有效性对照的「无检索来源」
     *                     臂据此只挂 {@link ChatTools#TOOL_FINISH}（消融变量只落在工具装配上）
     */
    public record Input(
            String question,
            @Nullable String sessionId,
            String userId,
            List<ChatTurn> history,
            List<UserPreference> prefs,
            @Nullable ChatStreamHandler streamHandler,
            @Nullable Set<String> allowedTools) {

        public Input(
                String question,
                @Nullable String sessionId,
                String userId,
                List<ChatTurn> history,
                List<UserPreference> prefs,
                @Nullable ChatStreamHandler streamHandler) {
            this(question, sessionId, userId, history, prefs, streamHandler, null);
        }
    }

    /**
     * 循环结果 — 召回物供生成装配与溯源，outcome / rounds 供指标与降级归因，toolPath 供路由准确率评估（对照金标准集
     * {@code expected_tools}）：rounds 含 finish 轮、不计决策失败轮，降级补检索不进 toolPath。
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

    // ── 入口与装配 ──

    public Result run(Input input) {
        ChatTools tools = toolsFactory.create(attributedUserId(input));
        ToolLoopKernel.Outcome outcome = loopKernel.run(spec(input, tools), chatDecider());
        return Result.of(tools, outcome.outcome(), outcome.rounds(), outcome.toolPath());
    }

    private ToolLoopKernel.Spec spec(Input input, ChatTools tools) {
        return new ToolLoopKernel.Spec(
                promptRegistry.require(CHAT_TOOL_PROMPT),
                firstUserMessage(input),
                toolCallbacks(tools, input.allowedTools()),
                aiProperties.chat().maxSteps(),
                budgetGuard::exhausted,
                () -> fallbackSearch(input, tools),
                input.sessionId(),
                attributedUserId(input),
                input.streamHandler(),
                metrics);
    }

    @Nullable
    private static String attributedUserId(Input input) {
        return MACHINE_SUBJECT.equals(input.userId()) ? null : input.userId();
    }

    /** 白名单在装配层生效 —— 模型拿不到工具 schema 就调不到。 */
    private static List<ToolCallback> toolCallbacks(ChatTools tools, @Nullable Set<String> allowedTools) {
        ToolCallback[] all = ToolCallbacks.from(tools);
        if (allowedTools == null) {
            return List.of(all);
        }
        return Stream.of(all)
                .filter(callback ->
                        allowedTools.contains(callback.getToolDefinition().name()))
                .toList();
    }

    // ── 首轮上下文 ──

    /**
     * 首条 user 消息（问题 / 历史 / 偏好）— 每请求固定、全部轮次共享：改一个字节这轮的 KV cache 就作废。三个分量剥掉
     * 标签形态后统一进块（与生成侧同形）—— 这条上下文决定调哪个工具，散文小标题能被块内用户文本仿写，剥掉后仿不出来。
     */
    private static String firstUserMessage(Input input) {
        String question = UntrustedText.stripTags(input.question());
        String history = UntrustedText.stripTags(formatHistory(input.history()));
        String preferences = UntrustedText.stripTags(UserPreference.format(input.prefs()));
        return """
                <user_question>
                %s
                </user_question>

                <history>
                %s
                </history>

                <user_preferences>
                %s
                </user_preferences>
                """.formatted(question, history, preferences);
    }

    private static String formatHistory(List<ChatTurn> history) {
        if (history.isEmpty()) {
            return "(无)";
        }
        return history.stream()
                .map(turn -> (turn.role().isUser() ? "用户" : "助手") + ": " + turn.content())
                .collect(Collectors.joining("\n"));
    }

    // ── 决策 ──

    /** 决策走 chat 场景快模型、记账进 chat 预算；入参摘要按本链路工具面取。 */
    private ToolLoopDecider chatDecider() {
        return (sessionId, messages, callbacks) ->
                decider.decideForLoop(sessionId, messages, callbacks, AiCallScope.CHAT, ToolCallLoop::toolInputOf);
    }

    /** 工具入参摘要（trace 落库与失败日志用）— 工具名缺失已在 {@link ToolCallDecision} 构造期收敛成空串，这里走 default，
     * 不再判空。 */
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

    // ── 降级 ──

    /** 决策失败的降级补检索 — 判重口径与工具步一致（在 {@code ChatTools} 内），自身故障不外抛（对话不死在降级路径）。 */
    private void fallbackSearch(Input input, ChatTools tools) {
        if (!allowsKnowledgeSearch(input)) {
            return;
        }
        try {
            tools.searchKnowledgeForFallback(input.question());
        } catch (Exception e) {
            log.warn(
                    "action=tool_call_fallback_search_failed, sessionId={}, reason={}",
                    input.sessionId(),
                    FailureReason.of(e));
        }
    }

    private static boolean allowsKnowledgeSearch(Input input) {
        Set<String> allowed = input.allowedTools();
        return allowed == null || allowed.contains(ChatTools.TOOL_KNOWLEDGE_SEARCH);
    }
}
