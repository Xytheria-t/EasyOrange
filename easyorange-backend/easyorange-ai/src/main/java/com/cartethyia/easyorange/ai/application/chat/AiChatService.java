package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.application.dto.ChatRequest;
import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.constant.AiResultCode;
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
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.framework.lock.DistributedLockPort;
import com.cartethyia.easyorange.framework.lock.LockAcquisitionException;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * AI 智能对话（Agent 编排）— 多轮记忆 + 多步工具循环 + 引用溯源 + 语义缓存 + 预算治理；
 * 与搜索页的结构化检索彻底正交：那边是「一次查询定结果」，这里是模型自己决定检索几轮、查什么。
 * <pre>
 * 1. 记忆装配：Redis 会话窗口（短期）+ 用户画像表（长期），历史注入前过 token 预算裁剪（{@link ChatContextTrimmer}）
 * 2. 工具循环：{@link AgentLoopRunner} 逐轮「决策 → 工具 → 观察」，模型判定信息足够（finish）收敛；
 *    步数 / 预算超限降级为「用已积累观察直接生成」，决策失败降级为按原始问题检索一次
 * 3. 生成回答：消息形状在 {@link ChatPromptAssembler}（system 注入画像/历史/知识/资产/详情），
 *    回答末尾 [来源:标题] 引用溯源
 * </pre>
 * 流式路径的预算前置检查不挂 {@link TokenBudget} 注解，由 {@link #streamAnswer} 手动执行（判定与循环中途
 * 共用 {@link AgentLoopRunner#chatBudgetExhausted()}，口径单处维护）。不是 AOP 拦不住——注解同样会被
 * 切面拦截——而是切面在<b>代理边界</b>抛 {@link TokenBudgetExceededException}，发生在方法体之前：
 * streamAnswer 内部把预算异常转成「预算已用尽」error 事件的路由接不到它，异常会落到 Controller 的
 * 通用兜底，用户看到的预算提示变成通用降级文案。
 */
@Slf4j
@Service
public class AiChatService {

    private static final String CHAT_PROMPT = "ai_chat_system";

    private static final String DEGRADED_METRIC = "easyorange.ai.chat.degraded";
    private static final String SESSION_BUSY_METRIC = "easyorange.ai.chat.session.busy";
    private static final String STREAM_ABORTED_METRIC = "easyorange.ai.chat.stream.aborted";

    /** 空问题的提示语：非流式走 {@link ChatAnswer}、流式走 SSE error 事件，同源一处维护。 */
    private static final String EMPTY_QUESTION_TEXT = "请描述你的问题";

    /**
     * 同会话请求撞上串行锁等待超时的提示语：非流式走 {@link ChatAnswer}、流式走 SSE error 事件。
     */
    private static final String SESSION_BUSY_TEXT = "上一条消息还在处理中，请稍候再试";

    /** 会话串行锁键前缀 —— 锁粒度 = 会话，后到请求排队到前一轮完整落盘之后。 */
    private static final String SESSION_LOCK_PREFIX = "eo:chat:session-lock:";

    /**
     * 引用来源下发条数上限。
     * <p>
     * 3 条是「够看清依据」与「不淹没回答」之间的取值；{@code product_detail} 的观察物不再单列 ——
     * 它查的就是 {@code product_search} 已经召回过的那些资产，重复下发只会挤掉新的召回。
     */
    private static final int SOURCE_LIMIT = 3;

    private final ChatModel chatModel;
    private final PromptRegistry promptRegistry;
    private final AiModelSupport aiModelSupport;
    private final SemanticCachePort semanticCache;
    private final ChatSessionPort sessionStore;
    private final UserPreferenceRepository preferenceRepository;
    private final AgentLoopRunner agentLoopRunner;
    private final ChatContextTrimmer contextTrimmer;
    private final DistributedLockPort distributedLockPort;
    private final AiProperties aiProperties;
    /** 值类型是 {@link ChatAnswer}：与 framework 的 {@code imageProcessCache} 按泛型区分，注入无需 {@code @Qualifier}。 */
    private final Cache<String, ChatAnswer> staleCache;

    private final MeterRegistry meterRegistry;

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

    /** 降级计数 —— 按原因全集注册，两条路径（非流式 / 流式）共用同一组计数器。 */
    private final Map<DegradationReason, Counter> degradedCounters;

    private final Counter sessionBusyCounter;

    private final Counter streamAbortedCounter;

    public AiChatService(
            ChatModel chatModel,
            PromptRegistry promptRegistry,
            AiModelSupport aiModelSupport,
            SemanticCachePort semanticCache,
            ChatSessionPort sessionStore,
            UserPreferenceRepository preferenceRepository,
            AgentLoopRunner agentLoopRunner,
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
        this.agentLoopRunner = agentLoopRunner;
        this.contextTrimmer = contextTrimmer;
        this.distributedLockPort = distributedLockPort;
        this.aiProperties = aiProperties;
        this.staleCache = staleCache;
        this.meterRegistry = meterRegistry;
        this.degradedCounters = new EnumMap<>(DegradationReason.class);
        for (DegradationReason reason : DegradationReason.values()) {
            degradedCounters.put(reason, meterRegistry.counter(DEGRADED_METRIC, "reason", reason.tag()));
        }
        this.sessionBusyCounter = meterRegistry.counter(SESSION_BUSY_METRIC);
        this.streamAbortedCounter = meterRegistry.counter(STREAM_ABORTED_METRIC);
    }

    /**
     * 非流式回答（语义缓存 + 预算 AOP + 故障降级）。
     * <p>
     * 两个缓存职责不同：**语义缓存**（Redis，跨请求近似问题复用）在 {@code forceFresh} 下读写都跳过，
     * 且一次请求只向量化一次；**stale 缓存**（本地 Caffeine，供应商故障兜底）随每次成功回答无条件刷新
     * —— {@code forceFresh} 只绕过语义缓存，故障时仍能拿到旧回答。
     * <p>
     * 供应商故障不抛异常：有 stale 旧回答就复用，没有就返回降级文案，两者都置 {@link ChatAnswer#degraded()}
     * 并计入 {@code easyorange.ai.chat.degraded}。抛出去只会变成 500 + 通用错误码：调用方读不到「AI 不可用」，
     * 错误率大盘也分不清供应商故障与代码缺陷，且与流式路径的 error 事件口径不一致。预算超限等业务异常照旧
     * 上抛——那是客户端可控的 4xx，不该伪装成降级回答。
     */
    @TokenBudget(scenario = "chat", maxTokensPerCall = 1500, dailyTokenLimit = 300_000)
    public ChatAnswer answer(ChatRequest request) {
        return answer(request, currentUserId());
    }

    /**
     * 非流式回答，调用方显式给定用户身份 —— SSE 那条路径工作在另一个线程，读不到本线程的
     * {@code SecurityContextHolder}（见 {@code streamAnswer}）。两条路径共用同一份实现，
     * 身份从入参拿而不是各自去读 ThreadLocal。
     */
    ChatAnswer answer(ChatRequest request, String userId) {
        if (request.question() == null || request.question().isBlank()) {
            return new ChatAnswer(EMPTY_QUESTION_TEXT, List.of(), request.sessionId(), false);
        }
        try {
            // 查询向量只算一次：命中查找与未命中后的写入共用（空列表 = 缓存开关关闭 / embedding 不可用）
            List<Float> queryEmbedding =
                    request.forceFresh() ? List.of() : semanticCache.embedQuery(request.question());
            if (!queryEmbedding.isEmpty()) {
                var cached = semanticCache.lookUp(
                        AiCallScope.CHAT, userId, request.question(), queryEmbedding, ChatAnswer.class);
                if (cached.isPresent()) {
                    return cached.get().withSessionId(request.sessionId());
                }
            }
            ChatAnswer answer = agenticAnswer(request, userId, null);
            if (!queryEmbedding.isEmpty()) {
                semanticCache.store(AiCallScope.CHAT, userId, request.question(), queryEmbedding, answer);
            }
            staleCache.put(staleKey(userId, request.question()), answer);
            return answer;
        } catch (LockAcquisitionException e) {
            log.warn("action=chat_session_busy, sessionId={}", request.sessionId());
            sessionBusyCounter.increment();
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
            log.error("action=chat_degraded, reason=unavailable, question={}", request.question(), e);
            degradedCounters.get(DegradationReason.UNAVAILABLE).increment();
            return ChatAnswer.unavailable(request.sessionId());
        }
    }

    /**
     * stale 缓存键 —— 与语义缓存同一分桶口径（{@link SemanticCachePort#cacheUserKey}），
     * 否则「供应商故障时兜底」会把一个人的旧回答返给另一个人。
     */
    private static String staleKey(String userId, String question) {
        return AiCallScope.CHAT.cacheKeyPrefix() + SemanticCachePort.cacheUserKey(userId) + ':' + question;
    }

    /** 调用线程上的登录身份 —— 非流式路径与 Controller 同线程，直接读安全上下文。 */
    static String currentUserId() {
        return SecurityContextUtil.getCurrentUserId().orElse(AgentLoopRunner.ANONYMOUS_USER);
    }

    /**
     * 流式回答（SSE）：step（Agent 步骤）→ token 逐段回调；错误统一走 {@link ChatStreamHandler#onError}。
     * <p>
     * 预算记账不在这里做：{@link AiModelSupport} 拿到流末帧的用量分片后按场景记账，切面只前置检查，
     * 两条路径记账口径一致。前置检查手动做而不挂 {@link TokenBudget} 注解——不是 AOP 拦不住（注解同样
     * 会被切面拦截），而是切面在代理边界抛 {@link TokenBudgetExceededException}，本方法内部把预算异常
     * 转成「预算已用尽」提示的路由接不到它（详见类注释）。
     * <p>
     * <b>身份由调用方传入而非在这里读安全上下文</b>：流式工作跑在 Controller 提交的另一个线程上，
     * {@code SecurityContextHolder} 的 ThreadLocal 不会跟着过去（Controller 在 servlet 线程上
     * 已把身份取出来传进来）。在这里读会恒为 anonymous —— 长期画像不加载、
     * {@code remember_preference} 写不进库、trace 的 userId 为空，而这条路径是前端唯一调用的路径。
     */
    public void streamAnswer(ChatRequest request, @Nullable AuthUser authUser, ChatStreamHandler handler) {
        if (request.question() == null || request.question().isBlank()) {
            handler.onError(EMPTY_QUESTION_TEXT);
            return;
        }
        String userId = authUser != null ? authUser.userId() : AgentLoopRunner.ANONYMOUS_USER;
        try {
            // 流式链路绕过 @TokenBudget 切面，这里手动前置检查；判定与循环中途共用 AgentLoopRunner 同一方法
            if (agentLoopRunner.chatBudgetExhausted()) {
                log.warn("action=token_budget_exceeded, scenario={}", AiCallScope.CHAT.budgetScenario());
                throw new TokenBudgetExceededException();
            }
            handler.onDone(agenticAnswer(request, userId, handler).answer());
        } catch (TokenBudgetExceededException e) {
            handler.onError("今日 AI 调用预算已用尽，请明天再试");
        } catch (LockAcquisitionException e) {
            log.warn("action=chat_session_busy, sessionId={}", request.sessionId());
            sessionBusyCounter.increment();
            handler.onError(SESSION_BUSY_TEXT);
        } catch (ChatStreamAbortedException e) {
            // 客户端中途离开（刷新/关页）：正常中断而非模型故障 —— 不打 ERROR、
            // 不计入 chat.degraded（否则每次刷新都虚高降级率）、不回 onError（事件已无听众）
            log.debug("action=chat_stream_aborted, question={}", request.question());
            streamAbortedCounter.increment();
        } catch (Exception e) {
            log.error("action=chat_degraded, reason=unavailable, question={}", request.question(), e);
            degradedCounters.get(DegradationReason.UNAVAILABLE).increment();
            handler.onError(ChatAnswer.UNAVAILABLE_TEXT);
        }
    }

    private ChatAnswer agenticAnswer(ChatRequest request, String userId, @Nullable ChatStreamHandler handler) {
        String sessionId = request.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            // 无会话即无共享状态，没有需要串行化的 load→save，直接执行
            return doAgenticAnswer(request, userId, handler);
        }
        // 同会话串行：load→loop→save 非原子，并发请求会互相串写历史（读到半轮、写丢轮）。
        // per-session 分布式锁（键带 userId，与记忆键同一归属口径）把后到请求排队到前一轮完整落盘之后；
        // 等待超时按业务提示返回，不伪装成模型降级。无事务上下文，锁在方法返回即释放（watchdog 覆盖整个持锁期）
        return distributedLockPort.executeWithLocks(
                List.of(SESSION_LOCK_PREFIX + userId + ":" + sessionId),
                aiProperties.chat().sessionLockWaitSeconds(),
                () -> doAgenticAnswer(request, userId, handler));
    }

    private ChatAnswer doAgenticAnswer(ChatRequest request, String userId, @Nullable ChatStreamHandler handler) {
        // token 级上下文治理：轮数窗口（存储侧）之上再按 token 预算裁注入窗口，一处裁、决策与生成两处生效
        List<ChatTurn> history = contextTrimmer.trim(sessionStore.loadRecent(userId, request.sessionId()));
        List<UserPreference> prefs =
                AgentLoopRunner.ANONYMOUS_USER.equals(userId) ? List.of() : preferenceRepository.findByUserId(userId);

        // 2. 多步工具循环（决策 → 工具 → 观察，步数/预算超限在循环内降级）
        AgentLoopRunner.Result run = agentLoopRunner.run(
                new AgentLoopRunner.Input(request.question(), request.sessionId(), userId, history, prefs, handler));

        // 引用来源：知识 / 资产两路召回物合并成结构化来源（带 type + id），资产优先，截断到 3 条
        List<ChatSource> sources = ChatSource.merge(run.knowledgeHits(), run.assets(), SOURCE_LIMIT);
        if (handler != null && !sources.isEmpty()) {
            handler.onSources(sources);
        }

        // 3. 生成回答（流式时逐 token 回调；消息形状见 ChatPromptAssembler）
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
                userId, request.sessionId(), List.of(ChatTurn.user(request.question()), ChatTurn.assistant(answer)));
        return new ChatAnswer(answer, sources, request.sessionId(), false);
    }
}
