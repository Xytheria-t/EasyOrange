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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
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
 * 3. 生成回答：system prompt 注入画像/历史/知识片段/资产/资产详情，回答末尾 [来源:标题] 引用溯源
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
        List<ChatTurn> history = contextTrimmer.trim(rawHistory).history();
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

        // 3. 生成回答（按角色传消息：system / 历史 user+assistant / 当前 user，流式时逐 token 回调）
        List<Message> messages =
                buildMessages(promptRegistry.require(CHAT_PROMPT), request.question(), history, prefs, run);
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

    /**
     * 组装生成回答的消息序列：system + 历史 user/assistant 轮次 + 当前 user（画像 / 检索结果 / 问题）。
     * <p>
     * 历史不进当前 user 消息 —— 跨轮次的前缀保持稳定，供应商上下文缓存（按前缀命中折扣计价）才有效，
     * 历史也按原始角色呈现（而不是压平成一段文本），模型对轮次的区分更准。
     * <p>
     * 用户问题与检索片段一律放进带标签的块：它们是数据不是指令，配合 system prompt 的约束，
     * 降低「商品描述/提问里写指令操纵模型」的成功率。
     */
    private static List<Message> buildMessages(
            String systemPrompt,
            String question,
            List<ChatTurn> history,
            List<UserPreference> prefs,
            AgentLoopRunner.Result run) {
        List<Message> messages = new ArrayList<>(history.size() + 2);
        messages.add(new SystemMessage(systemPrompt));
        for (ChatTurn turn : history) {
            messages.add(turn.role().isUser()
                    ? new UserMessage(turn.content())
                    : new AssistantMessage(turn.content()));
        }
        messages.add(new UserMessage(buildCurrentUserMessage(question, prefs, run)));
        return messages;
    }

    private static String buildCurrentUserMessage(
            String question, List<UserPreference> prefs, AgentLoopRunner.Result run) {
        return """
                <user_question>
                %s
                </user_question>

                <user_profile>
                %s
                </user_profile>

                <knowledge_snippets>
                %s
                </knowledge_snippets>

                <candidate_assets>
                %s
                </candidate_assets>

                <asset_details>
                %s
                </asset_details>
                """.formatted(
                        question,
                        UserPreference.format(prefs),
                        formatHits(run.knowledgeHits()),
                        formatAssets(run.assets()),
                        formatDetails(run.details()));
    }

    private static String formatHits(List<KnowledgeHit> hits) {
        if (hits.isEmpty()) {
            return "(无检索结果)";
        }
        var sb = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            KnowledgeHit hit = hits.get(i);
            sb.append("[%d] (%s)\n%s\n".formatted(i + 1, hit.title(), hit.content()));
        }
        return sb.toString();
    }

    /**
     * 资产块带 id 与价格：模型据此写推荐理由，而 id 是回答「推荐的确实是真实在售资产」的校验锚点
     * —— 提示词已硬约束不得编造资产与数字，这里再把可核对的信息（id）显式给到，让约束有据可依。
     */
    private static String formatAssets(List<AssetHit> assets) {
        if (assets.isEmpty()) {
            return "(无可推荐资产)";
        }
        var sb = new StringBuilder();
        for (AssetHit asset : assets) {
            sb.append("[%s] %s | ¥%s | %s | %s\n"
                    .formatted(
                            asset.productId(),
                            asset.title(),
                            asset.price() == null
                                    ? "面议"
                                    : asset.price().stripTrailingZeros().toPlainString(),
                            asset.categoryName() == null ? "未分类" : asset.categoryName(),
                            asset.conditionDesc() == null ? "成色未标注" : asset.conditionDesc()));
        }
        return sb.toString();
    }

    /**
     * 详情块承接 product_detail 轮次的观察：描述全文进 prompt，模型对某件资产的推荐理由
     * 才有据可写（资产块里只有标题 / 价格 / 成色一行摘要）。
     */
    private static String formatDetails(List<AssetDetail> details) {
        if (details.isEmpty()) {
            return "(无)";
        }
        var sb = new StringBuilder();
        for (AssetDetail detail : details) {
            sb.append("[%s] %s\n描述：%s\n"
                    .formatted(
                            detail.productId(),
                            detail.title(),
                            detail.description() == null ? "无描述" : detail.description()));
        }
        return sb.toString();
    }
}
