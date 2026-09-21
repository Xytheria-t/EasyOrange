package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.application.dto.ChatRequest;
import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.exception.TokenBudgetExceededException;
import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.UserPreference;
import com.cartethyia.easyorange.ai.domain.port.ChatSessionPort;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import com.cartethyia.easyorange.ai.domain.port.SemanticCachePort;
import com.cartethyia.easyorange.ai.domain.port.UserPreferenceRepository;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * AI 智能对话（Agent 编排）— 多轮记忆 + 多步工具循环 + 知识库引用溯源 + 语义缓存 + 预算治理。
 * <p>
 * 编排结构（多步 ReAct，循环体在 {@link AgentLoopRunner}，与 4 路并行编排
 * {@code AiSearchEnhancerAdapter} 形成「Workflow vs 自治 Agent」对照）：
 * <pre>
 * 1. 记忆装配：Redis 会话窗口（短期）+ 用户画像表（长期），历史注入前过 token 预算裁剪（{@link ChatContextTrimmer}）
 * 2. 工具循环：{@link AgentLoopRunner} 逐轮「决策 → 工具 → 观察」，模型判定信息足够（finish）收敛；
 *    步数 / 预算超限降级为「用已积累观察直接生成」，决策失败降级为按原始问题检索一次
 * 3. 生成回答：system prompt 注入画像/历史/知识片段/资产/资产详情（消息装配在 {@link ChatPromptAssembler}），
 *    回答末尾 [来源:标题] 引用溯源
 * </pre>
 * 流式路径（SSE）在方法返回前完成不了 AOP 预算记账，由 {@link #streamAnswer}
 * 手动执行与 {@link TokenBudget} 相同的预算检查（判定与循环中途共用
 * {@link AgentLoopRunner#chatBudgetExhausted()}，口径单处维护）。
 */
@Slf4j
@Service
public class AiChatService {

    private static final String CHAT_PROMPT = "ai_chat_system";

    /** 空问题的提示语：非流式走 {@link ChatAnswer}、流式走 SSE error 事件，同源一处维护。 */
    private static final String EMPTY_QUESTION_TEXT = "请描述你的问题";

    private final ChatModel chatModel;
    private final PromptRegistry promptRegistry;
    private final AiModelSupport aiModelSupport;
    private final SemanticCachePort semanticCache;
    private final ChatSessionPort sessionStore;
    private final UserPreferenceRepository preferenceRepository;
    private final AgentLoopRunner agentLoopRunner;
    private final ChatContextTrimmer contextTrimmer;
    private final AiProperties aiProperties;
    private final Cache<String, Object> staleCache;
    private final MeterRegistry meterRegistry;

    // Lombok 构造器不会把 @Qualifier 复制到参数上，故手写显式构造器以保留 "aiStaleCache" 限定
    public AiChatService(
            ChatModel chatModel,
            PromptRegistry promptRegistry,
            AiModelSupport aiModelSupport,
            SemanticCachePort semanticCache,
            ChatSessionPort sessionStore,
            UserPreferenceRepository preferenceRepository,
            AgentLoopRunner agentLoopRunner,
            ChatContextTrimmer contextTrimmer,
            AiProperties aiProperties,
            @Qualifier("aiStaleCache") Cache<String, Object> staleCache,
            MeterRegistry meterRegistry) {
        this.chatModel = chatModel;
        this.promptRegistry = promptRegistry;
        this.aiModelSupport = aiModelSupport;
        this.semanticCache = semanticCache;
        this.sessionStore = sessionStore;
        this.preferenceRepository = preferenceRepository;
        this.agentLoopRunner = agentLoopRunner;
        this.contextTrimmer = contextTrimmer;
        this.aiProperties = aiProperties;
        this.staleCache = staleCache;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 非流式回答（语义缓存 + 预算 AOP + 故障降级）。
     * <p>
     * 两个缓存职责不同：**语义缓存**（Redis，近似问题跨请求复用）在 {@code forceFresh} 下读写都跳过，
     * 且一次请求只向量化一次；**stale 缓存**（本地 Caffeine，供应商故障时兜底）随每次成功回答
     * 无条件刷新 —— {@code forceFresh} 只是绕过语义缓存，故障时仍能拿到旧回答。
     * <p>
     * 供应商故障不抛异常：有 stale 旧回答就复用它，没有就返回降级文案，两者都把
     * {@link ChatAnswer#degraded()} 置真并计入 {@code easyorange.ai.chat.degraded} ——
     * 抛出去只会变成 500 + 通用错误码（调用方读不到「AI 不可用」这个语义，错误率大盘也分不清
     * 供应商故障与代码缺陷），与流式路径的 error 事件口径不一致。预算超限等业务异常照旧上抛，
     * 那是客户端可控的 4xx，不该伪装成降级回答。
     */
    @TokenBudget(scenario = "chat", maxTokensPerCall = 1500, dailyTokenLimit = 300_000)
    public ChatAnswer answer(ChatRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return new ChatAnswer(EMPTY_QUESTION_TEXT, List.of(), request.sessionId(), false);
        }
        try {
            // 查询向量只算一次：命中查找与未命中后的写入共用（空列表 = 缓存开关关闭 / embedding 不可用）
            List<Float> queryEmbedding =
                    request.forceFresh() ? List.of() : semanticCache.embedQuery(request.question());
            if (!queryEmbedding.isEmpty()) {
                var cached =
                        semanticCache.lookUp(AiCallScope.CHAT, request.question(), queryEmbedding, ChatAnswer.class);
                if (cached.isPresent()) {
                    return cached.get().withSessionId(request.sessionId());
                }
            }
            ChatAnswer answer = agenticAnswer(request, null);
            if (!queryEmbedding.isEmpty()) {
                semanticCache.store(AiCallScope.CHAT, request.question(), queryEmbedding, answer);
            }
            staleCache.put(staleKey(request.question()), answer);
            return answer;
        } catch (BaseBusinessException e) {
            throw e;
        } catch (Exception e) {
            Object stale = staleCache.getIfPresent(staleKey(request.question()));
            if (stale instanceof ChatAnswer cached) {
                log.warn(
                        "action=chat_degraded, reason=stale, question={}, cause={}",
                        request.question(),
                        e.getMessage());
                degradedCounter("stale").increment();
                return cached.asDegraded().withSessionId(request.sessionId());
            }
            log.error("action=chat_degraded, reason=unavailable, question={}", request.question(), e);
            degradedCounter("unavailable").increment();
            return ChatAnswer.unavailable(request.sessionId());
        }
    }

    private Counter degradedCounter(String reason) {
        return meterRegistry.counter("easyorange.ai.chat.degraded", "reason", reason);
    }

    private static String staleKey(String question) {
        return AiCallScope.CHAT.cacheKeyPrefix() + question;
    }

    /**
     * 流式回答（SSE）：step（Agent 步骤）→ token 逐段回调；错误统一走 {@link ChatStreamHandler#onError}。
     * <p>
     * 预算记账不在这里做：{@link AiModelSupport} 拿到流末帧的用量分片后按场景记账，
     * {@link #checkBudget()} 只负责前置检查（本方法不带 {@link TokenBudget} 注解，AOP 拦不住，
     * 若两边都记会重复计数）。
     */
    public void streamAnswer(ChatRequest request, ChatStreamHandler handler) {
        if (request.question() == null || request.question().isBlank()) {
            handler.onError(EMPTY_QUESTION_TEXT);
            return;
        }
        try {
            checkBudget();
            ChatAnswer answer = agenticAnswer(request, handler);
            handler.onDone(answer.answer());
        } catch (TokenBudgetExceededException e) {
            handler.onError("今日 AI 调用预算已用尽，请明天再试");
        } catch (Exception e) {
            log.error("action=chat_degraded, reason=unavailable, question={}", request.question(), e);
            degradedCounter("unavailable").increment();
            handler.onError(ChatAnswer.UNAVAILABLE_TEXT);
        }
    }

    private ChatAnswer agenticAnswer(ChatRequest request, @Nullable ChatStreamHandler handler) {
        String userId = SecurityContextUtil.getCurrentUserId().orElse(AgentLoopRunner.ANONYMOUS_USER);
        List<ChatTurn> rawHistory =
                sessionStore.loadRecent(request.sessionId(), aiProperties.chat().historyLimit());
        // token 级上下文治理：轮数窗口（存储侧）之上再按 token 预算裁注入窗口，一处裁、决策与生成两处生效
        List<ChatTurn> history = contextTrimmer.trim(rawHistory);
        List<UserPreference> prefs =
                AgentLoopRunner.ANONYMOUS_USER.equals(userId) ? List.of() : preferenceRepository.findByUserId(userId);

        // 2. 多步工具循环（决策 → 工具 → 观察，步数/预算超限在循环内降级）
        AgentLoopRunner.Result run = agentLoopRunner.run(
                new AgentLoopRunner.Input(request.question(), request.sessionId(), userId, history, prefs, handler));

        List<String> sources = Stream.concat(
                        Stream.concat(
                                run.knowledgeHits().stream().map(KnowledgeHit::title),
                                run.assets().stream().map(AssetHit::title)),
                        run.details().stream().map(AssetDetail::title))
                .distinct()
                .limit(3)
                .toList();
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
            throw new IllegalStateException("AI returned empty answer");
        }

        // 一轮对话一次写入（提问 + 回答），存储侧一次落盘也不会留下半轮记忆
        sessionStore.saveTurns(
                request.sessionId(), List.of(ChatTurn.user(request.question()), ChatTurn.assistant(answer)));
        return new ChatAnswer(answer, sources, request.sessionId(), false);
    }

    private void checkBudget() {
        if (agentLoopRunner.chatBudgetExhausted()) {
            // 流式链路绕过 @TokenBudget 切面，这里手动检查 —— 判定与循环中途共用 AgentLoopRunner 同一方法
            log.warn("action=token_budget_exceeded, scenario={}", AiCallScope.CHAT.budgetScenario());
            throw new TokenBudgetExceededException();
        }
    }
}
