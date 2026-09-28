package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.application.dto.ChatRequest;
import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.application.support.ChatBudgetGuard;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import com.cartethyia.easyorange.ai.domain.enums.AiResultCode;
import com.cartethyia.easyorange.ai.domain.exception.TokenBudgetExceededException;
import com.cartethyia.easyorange.ai.domain.model.ChatSource;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.domain.model.UserPreference;
import com.cartethyia.easyorange.ai.domain.port.ChatSessionPort;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamAbortedException;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import com.cartethyia.easyorange.ai.domain.port.SemanticCachePort;
import com.cartethyia.easyorange.ai.domain.port.UserPreferenceRepository;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.lock.DistributedLockPort;
import com.cartethyia.easyorange.framework.lock.LockAcquisitionException;
import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * AI 智能对话（工具循环编排）— 多轮记忆 + 多步工具循环 + 引用溯源 + 语义缓存 + 预算治理；与搜索页的
 * 结构化检索正交：那边「一次查询定结果」，这里模型自己决定检索几轮、查什么。
 * <p>
 * 链路：记忆装配（Redis 会话窗口 + 用户画像表，注入前过 {@link ChatContextTrimmer} 的 token 裁剪）→
 * 工具循环（{@link ToolCallLoop}，步数 / 预算超限降级为用已积累观察直接生成，决策失败降级为按原始
 * 问题检索一次）→ 生成回答（消息形状在 {@link ChatPromptAssembler}，回答末尾 [来源:标题] 引用溯源）。
 * <p>
 * 流式路径的预算前置检查手动做而不挂 {@link TokenBudget} 注解（原因见 {@link ChatBudgetGuard}）：
 * 不是 AOP 拦不住——注解同样会被切面拦截——而是切面在<b>代理边界</b>抛
 * {@link TokenBudgetExceededException}，发生在方法体之前，streamAnswer 内部把预算异常转成 error
 * 事件的路由接不到它，预算提示会落成 Controller 的通用降级文案。
 */
@Slf4j
@Service
public class AiChatAppService {

    private static final String CHAT_PROMPT = "ai_chat_system";

    private static final String DEGRADED_METRIC = "easyorange.ai.chat.degraded";
    private static final String SESSION_BUSY_METRIC = "easyorange.ai.chat.session.busy";
    private static final String STREAM_ABORTED_METRIC = "easyorange.ai.chat.stream.aborted";
    /**
     * 一次问答的端到端耗时（工具调用循环 + 生成 + 会话落盘）— 对外引用的延迟数字只有这一处指标源。
     * 口径只圈「跑通生成链路」的请求：语义缓存命中与空问题不进分布，否则几十毫秒的缓存回放会把
     * p95 拉到与真实生成延迟不可比；直方图分位数在多副本下由 PromQL 聚合。
     */
    private static final String TURN_DURATION_METRIC = "easyorange.ai.chat.turn.duration";

    /** 空问题 / 同会话锁等待超时的提示语：非流式走 {@link ChatAnswer}、流式走 SSE error 事件，同源一处维护。 */
    private static final String EMPTY_QUESTION_TEXT = "请描述你的问题";

    private static final String SESSION_BUSY_TEXT = "上一条消息还在处理中，请稍候再试";

    /** 会话串行锁键前缀 —— 锁粒度 = 会话，后到请求排队到前一轮完整落盘之后。 */
    private static final String SESSION_LOCK_PREFIX = "eo:chat:session-lock:";

    /** 引用来源下发条数上限 — product_detail 的观察物不单列：它查的就是 product_search 已召回的资产，重复下发挤掉新召回。 */
    private static final int SOURCE_LIMIT = 3;

    private static final int CACHE_REPLAY_CHUNK_CHARS = 8;

    private final ChatModel chatModel;
    private final PromptRegistry promptRegistry;
    private final AiModelSupport aiModelSupport;
    private final SemanticCachePort semanticCache;
    private final ChatSessionPort sessionStore;
    private final UserPreferenceRepository preferenceRepository;
    private final ToolCallLoop toolCallLoop;
    private final ChatBudgetGuard budgetGuard;
    private final ChatContextTrimmer contextTrimmer;
    private final DistributedLockPort distributedLockPort;
    private final AiProperties aiProperties;
    /** 值类型是 {@link ChatAnswer}：与 framework 的 {@code imageProcessCache} 按泛型区分，注入无需 {@code @Qualifier}。 */
    private final Cache<String, ChatAnswer> staleCache;

    /**
     * 降级原因 — {@link #DEGRADED_METRIC} 的封闭 tag 集，构造期按全集注册计数器（新增原因自动带上，
     * 热路径零查找）；对外 tag 值是时序契约，固化在字段上，改枚举名不改 tag。
     */
    private enum DegradationReason {
        /** 供应商故障，复用 stale 旧回答兜底。 */
        STALE("stale"),
        /** 供应商故障且无旧回答可兜底，返回统一降级文案。 */
        UNAVAILABLE("unavailable");

        private final String tag;

        DegradationReason(String tag) {
            this.tag = tag;
        }

        String tag() {
            return tag;
        }
    }

    /** 按原因全集注册，两条路径（非流式 / 流式）共用同一组计数器。 */
    private final Map<DegradationReason, Counter> degradedCounters;

    private final Counter sessionBusyCounter;

    private final Counter streamAbortedCounter;

    private final Timer turnTimer;

    public AiChatAppService(
            ChatModel chatModel,
            PromptRegistry promptRegistry,
            AiModelSupport aiModelSupport,
            SemanticCachePort semanticCache,
            ChatSessionPort sessionStore,
            UserPreferenceRepository preferenceRepository,
            ToolCallLoop toolCallLoop,
            ChatBudgetGuard budgetGuard,
            ChatContextTrimmer contextTrimmer,
            DistributedLockPort distributedLockPort,
            AiProperties aiProperties,
            Cache<String, ChatAnswer> staleCache,
            MeterRegistry meterRegistry) {
        this.chatModel = chatModel;
        this.promptRegistry = promptRegistry;
        this.aiModelSupport = aiModelSupport;
        this.semanticCache = semanticCache;
        this.sessionStore = sessionStore;
        this.preferenceRepository = preferenceRepository;
        this.toolCallLoop = toolCallLoop;
        this.budgetGuard = budgetGuard;
        this.contextTrimmer = contextTrimmer;
        this.distributedLockPort = distributedLockPort;
        this.aiProperties = aiProperties;
        this.staleCache = staleCache;
        this.degradedCounters = new EnumMap<>(DegradationReason.class);
        for (DegradationReason reason : DegradationReason.values()) {
            degradedCounters.put(reason, meterRegistry.counter(DEGRADED_METRIC, "reason", reason.tag()));
        }
        this.sessionBusyCounter = meterRegistry.counter(SESSION_BUSY_METRIC);
        this.streamAbortedCounter = meterRegistry.counter(STREAM_ABORTED_METRIC);
        this.turnTimer = Timer.builder(TURN_DURATION_METRIC)
                .description("一次问答端到端耗时（循环 + 生成 + 落盘，不含缓存命中与锁等待）")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    /**
     * 非流式回答 — 供应商故障不抛异常：有 stale 旧回答就复用，没有就返回降级文案，两者都置
     * {@link ChatAnswer#degraded()} 并计入降级指标。抛出去只会变成 500 + 通用错误码：调用方读不到
     * 「AI 不可用」，大盘也分不清供应商故障与代码缺陷，且与流式路径的 error 事件口径不一致。
     * 预算超限等业务异常照旧上抛——客户端可控的 4xx，不伪装成降级回答。
     * <p>
     * 身份由入站适配层解析后显式传入（本类不读安全上下文）：HTTP 入口在 servlet 线程取登录态
     * （缺失即 401，身份是硬前置不静默降级，对话没有匿名路径），评估跑批传机器主体
     * （{@link ToolCallLoop#MACHINE_SUBJECT}）。入口见 Controller 与 {@code GoldenSetEvaluator}。
     * <p>
     * 两个缓存职责不同：语义缓存（Redis，跨请求近似问题复用）在 {@code forceFresh} 下读写都跳过；
     * stale 缓存（Caffeine，供应商故障兜底）随每次成功回答无条件刷新 —— forceFresh 只绕过语义缓存，
     * 故障时仍能拿到旧回答。
     */
    @TokenBudget(scenario = "chat", maxTokensPerCall = 1500, dailyTokenLimit = 300_000)
    public ChatAnswer answer(ChatRequest request, String userId) {
        if (request.question() == null || request.question().isBlank()) {
            return new ChatAnswer(EMPTY_QUESTION_TEXT, List.of(), request.sessionId(), false);
        }
        try {
            SemanticCacheProbe probe = probeSemanticCache(request, userId);
            if (probe.hit().isPresent()) {
                return probe.hit().get().withSessionId(request.sessionId());
            }
            ChatAnswer answer = answerWithSessionLock(request, userId, null);
            storeInSemanticCache(request, userId, probe, answer);
            staleCache.put(staleKey(userId, request.question()), answer);
            return answer;
        } catch (LockAcquisitionException e) {
            recordSessionBusy(request.sessionId());
            return new ChatAnswer(SESSION_BUSY_TEXT, List.of(), request.sessionId(), false);
        } catch (BaseBusinessException e) {
            throw e;
        } catch (Exception e) {
            ChatAnswer stale = staleCache.getIfPresent(staleKey(userId, request.question()));
            if (stale != null) {
                log.warn(
                        "action=chat_degraded, reason=stale, question={}, cause={}",
                        request.question(),
                        e.getMessage());
                degradedCounters.get(DegradationReason.STALE).increment();
                return stale.asDegraded().withSessionId(request.sessionId());
            }
            recordUnavailableDegradation(request.question(), e);
            return ChatAnswer.unavailable(request.sessionId());
        }
    }

    /**
     * 流式回答（SSE）：step → token 逐段回调，错误统一走 {@link ChatStreamHandler#onError}。
     * 语义缓存与非流式同一模式（embed 一次、命中回放、未命中写回）：命中把缓存回答按固定块回放成
     * token 事件并重放 sources 事件（不进循环，无 step 事件），缓存命中不碰会话记忆、也不进会话锁
     * ——没有需要串行化的共享状态；流末帧用量由 {@link AiModelSupport} 记账，切面只前置检查。
     * <p>
     * 身份由调用方显式传入而非在这里读安全上下文：流式跑在 Controller 提交的另一线程上，
     * {@code SecurityContextHolder} 的 ThreadLocal 不会跟着过去，在这里读会恒为空 ——
     * 长期画像不加载、偏好写不进库、trace 的 userId 为空。Controller 在 servlet 线程解析登录态后
     * 传入，缺失即 401（身份是硬前置，不静默降级）。对话链路没有匿名调用方，另一个主体是评估跑批
     * （{@link ToolCallLoop#MACHINE_SUBJECT}）。预算前置检查不挂注解的原因见类注释。
     */
    public void streamAnswer(ChatRequest request, String userId, ChatStreamHandler handler) {
        if (request.question() == null || request.question().isBlank()) {
            handler.onError(EMPTY_QUESTION_TEXT);
            return;
        }
        try {
            // 流式链路绕过 @TokenBudget 切面，这里手动前置检查；判定与循环中途共用 ChatBudgetGuard 同一方法
            if (budgetGuard.exhausted()) {
                log.warn("action=token_budget_exceeded, scenario={}", AiCallScope.CHAT.budgetScenario());
                throw new TokenBudgetExceededException();
            }
            SemanticCacheProbe probe = probeSemanticCache(request, userId);
            if (probe.hit().isPresent()) {
                replayCached(probe.hit().get(), handler);
                return;
            }
            ChatAnswer answer = answerWithSessionLock(request, userId, handler);
            storeInSemanticCache(request, userId, probe, answer);
            handler.onDone(answer.answer());
        } catch (TokenBudgetExceededException e) {
            handler.onError("今日 AI 调用预算已用尽，请明天再试");
        } catch (LockAcquisitionException e) {
            recordSessionBusy(request.sessionId());
            handler.onError(SESSION_BUSY_TEXT);
        } catch (ChatStreamAbortedException e) {
            // 客户端中途离开（刷新/关页）是正常中断而非模型故障：不打 ERROR、不计入 chat.degraded
            // （否则每次刷新都虚高降级率）、不回 onError（事件已无听众）
            log.debug("action=chat_stream_aborted, question={}", request.question());
            streamAbortedCounter.increment();
        } catch (Exception e) {
            recordUnavailableDegradation(request.question(), e);
            handler.onError(ChatAnswer.UNAVAILABLE_TEXT);
        }
    }

    /**
     * 语义缓存的一次探查 — 查询向量与命中结果打在一起返回：非流式 / 流式两条路径同一模式
     * （向量只算一次，未命中后的写回复用同一个向量），三段「算向量 → 查 → 写」因此只有这一份实现。
     * <p>
     * {@code queryEmbedding} 为空同时覆盖两件事：{@code forceFresh} 绕过语义缓存，以及 embedding 不可用
     * ——两条路径都让查找与写入一起跳过，命中恒为空、也不会把不可用写成脏缓存。
     */
    private record SemanticCacheProbe(List<Float> queryEmbedding, Optional<ChatAnswer> hit) {}

    private SemanticCacheProbe probeSemanticCache(ChatRequest request, String userId) {
        if (request.forceFresh()) {
            return new SemanticCacheProbe(List.of(), Optional.empty());
        }
        List<Float> queryEmbedding = semanticCache.embedQuery(request.question());
        if (queryEmbedding.isEmpty()) {
            return new SemanticCacheProbe(List.of(), Optional.empty());
        }
        return new SemanticCacheProbe(
                queryEmbedding,
                semanticCache.lookUp(AiCallScope.CHAT, userId, request.question(), queryEmbedding, ChatAnswer.class));
    }

    /** 未命中后的写回 — 跳过判据与 {@link #probeSemanticCache} 同源（空向量即不写），不做第二次向量化。 */
    private void storeInSemanticCache(ChatRequest request, String userId, SemanticCacheProbe probe, ChatAnswer answer) {
        if (!probe.queryEmbedding().isEmpty()) {
            semanticCache.store(AiCallScope.CHAT, userId, request.question(), probe.queryEmbedding(), answer);
        }
    }

    /** stale 缓存键 — 与语义缓存同一分桶口径（都按 userId），否则故障兜底会把一个人的旧回答返给另一个人。 */
    private static String staleKey(String userId, String question) {
        return AiCallScope.CHAT.cacheKeyPrefix() + userId + ':' + question;
    }

    /** 缓存命中回放 — sources 先行（与实时生成的事件顺序一致），按固定块推 token；不补人为延迟，缓存命中的价值就是快。 */
    private void replayCached(ChatAnswer cached, ChatStreamHandler handler) {
        if (!cached.sources().isEmpty()) {
            handler.onSources(cached.sources());
        }
        String answer = cached.answer();
        for (int i = 0; i < answer.length(); i += CACHE_REPLAY_CHUNK_CHARS) {
            handler.onToken(answer.substring(i, Math.min(i + CACHE_REPLAY_CHUNK_CHARS, answer.length())));
        }
        handler.onDone(answer);
    }

    /** 会话锁忙的日志与计数 — 非流式 / 流式两条路径共用一处，消息单点维护。 */
    private void recordSessionBusy(String sessionId) {
        log.warn("action=chat_session_busy, sessionId={}", sessionId);
        sessionBusyCounter.increment();
    }

    /** 不可用降级的日志与计数 — 同样两条路径共用，日志字段（question + 堆栈）只在此一处改。 */
    private void recordUnavailableDegradation(String question, Exception cause) {
        log.error("action=chat_degraded, reason=unavailable, question={}", question, cause);
        degradedCounters.get(DegradationReason.UNAVAILABLE).increment();
    }

    private ChatAnswer answerWithSessionLock(ChatRequest request, String userId, @Nullable ChatStreamHandler handler) {
        String sessionId = request.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            // 无会话即无共享状态，没有需要串行化的 load→save，直接执行
            return generateWithToolLoop(request, userId, handler);
        }
        // 同会话串行：load→loop→save 非原子，并发请求会互相串写历史（读到半轮、写丢轮）；
        // per-session 分布式锁把后到请求排队到前一轮完整落盘之后，等待超时按业务提示返回不伪装成降级。
        // 无事务上下文，锁在方法返回即释放（watchdog 覆盖整个持锁期）
        return distributedLockPort.executeWithLocks(
                List.of(SESSION_LOCK_PREFIX + userId + ":" + sessionId),
                aiProperties.chat().sessionLockWaitSeconds(),
                () -> generateWithToolLoop(request, userId, handler));
    }

    private ChatAnswer generateWithToolLoop(ChatRequest request, String userId, @Nullable ChatStreamHandler handler) {
        long start = System.nanoTime();
        try {
            // token 级上下文治理：轮数窗口（存储侧）之上再按 token 预算裁注入窗口，一处裁、决策与生成两处生效
            List<ChatTurn> history = contextTrimmer.trim(sessionStore.loadRecent(userId, request.sessionId()));
            // 机器主体在写侧被 ChatTools 拒收偏好，此处查到的恒为空表，无需特判
            List<UserPreference> prefs = preferenceRepository.findByUserId(userId);

            ToolCallLoop.Result run = toolCallLoop.run(
                    new ToolCallLoop.Input(request.question(), request.sessionId(), userId, history, prefs, handler));

            // 引用来源：知识 / 资产两路召回物合并成结构化来源（带 type + id），资产优先，截断到 3 条
            List<ChatSource> sources = ChatSource.merge(run.knowledgeHits(), run.assetHits(), SOURCE_LIMIT);
            if (handler != null && !sources.isEmpty()) {
                handler.onSources(sources);
            }

            List<Message> messages = ChatPromptAssembler.assemble(
                    promptRegistry.require(CHAT_PROMPT), request.question(), history, prefs, run);
            String answer = handler != null
                    ? aiModelSupport.callTextStream(chatModel, AiCallScope.CHAT, messages, handler::onToken)
                    : aiModelSupport.callText(chatModel, AiCallScope.CHAT, messages);
            if (answer == null || answer.isBlank()) {
                // 显式业务码而非裸 IllegalStateException（曾落 500）：空回答 = 模型没给出可用结果
                throw BusinessException.of(AiResultCode.AI_UNAVAILABLE, "模型返回空回答");
            }

            // 一轮对话一次写入（提问 + 回答），存储侧一次落盘也不会留下半轮记忆
            sessionStore.saveTurns(
                    userId,
                    request.sessionId(),
                    List.of(ChatTurn.user(request.question()), ChatTurn.assistant(answer)));
            return new ChatAnswer(answer, sources, request.sessionId(), false);
        } finally {
            turnTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }
}
