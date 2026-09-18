package com.cartethyia.easyorange.ai.application.service;

import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.application.dto.ChatRequest;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.exception.TokenBudgetExceededException;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.ToolDecision;
import com.cartethyia.easyorange.ai.domain.model.UserPreference;
import com.cartethyia.easyorange.ai.domain.port.ChatSessionPort;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import com.cartethyia.easyorange.ai.domain.port.SemanticCachePort;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStore;
import com.cartethyia.easyorange.ai.domain.port.UserPreferenceRepository;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
import tools.jackson.databind.ObjectMapper;

/**
 * AI 智能对话（Agent 编排）— 多轮记忆 + 工具调用 + 知识库引用溯源 + 语义缓存 + 预算治理。
 * <p>
 * 编排结构（单步 ReAct，编排手写不依赖框架黑盒）：
 * <pre>
 * 1. 记忆装配：Redis 会话窗口（短期）+ 用户画像表（长期）
 * 2. 工具决策：模型输出 JSON 决定本轮检索什么（知识库 / 在售资产 / 两者 / 不检索），顺带提取用户偏好
 * 3. 执行工具：{@link KnowledgeRetrievalService}（平台规则，两路召回 + RRF）与
 *    {@link AssetSourcingService}（在售资产，ONLINE 过滤）—— 两者是同一形态的不同语料，
 *    RAG 链路与引用溯源一行没改，换的只是检索对象
 * 4. 生成回答：system prompt 注入画像/历史/两类检索结果，回答末尾 [来源:标题] 引用溯源
 * </pre>
 * 流式路径（SSE）在方法返回前完成不了 AOP 预算记账，由 {@link #streamAnswer}
 * 手动执行与 {@link TokenBudget} 相同的预算检查。
 */
@Slf4j
@Service
public class AiChatService {

    private static final String CHAT_PROMPT = "ai_chat_system";
    private static final String TOOL_PROMPT = "ai_chat_tool_system";
    private static final String CHAT_SCENARIO = "chat";
    private static final int DEFAULT_MAX_TOKENS = 1500;
    private static final int DEFAULT_DAILY_LIMIT = 300_000;
    private static final int RETRIEVAL_TOP_K = 5;
    private static final int ASSET_TOP_K = 5;
    private static final String TOOL_KNOWLEDGE_SEARCH = "knowledge_search";
    private static final String TOOL_PRODUCT_SEARCH = "product_search";
    private static final String TOOL_BOTH = "both";
    private static final String ANONYMOUS_USER = "anonymous";

    private final ChatModel chatModel;
    private final PromptRegistry promptRegistry;
    private final AiModelSupport aiModelSupport;
    private final SemanticCachePort semanticCache;
    private final ChatSessionPort sessionStore;
    private final UserPreferenceRepository preferenceRepository;
    private final KnowledgeRetrievalService retrievalService;
    private final AssetSourcingService assetSourcingService;
    private final AiModelRouter modelRouter;
    private final TokenBudgetStore budgetStore;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
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
            KnowledgeRetrievalService retrievalService,
            AssetSourcingService assetSourcingService,
            AiModelRouter modelRouter,
            TokenBudgetStore budgetStore,
            AiProperties aiProperties,
            ObjectMapper objectMapper,
            @Qualifier("aiStaleCache") Cache<String, Object> staleCache,
            MeterRegistry meterRegistry) {
        this.chatModel = chatModel;
        this.promptRegistry = promptRegistry;
        this.aiModelSupport = aiModelSupport;
        this.semanticCache = semanticCache;
        this.sessionStore = sessionStore;
        this.preferenceRepository = preferenceRepository;
        this.retrievalService = retrievalService;
        this.assetSourcingService = assetSourcingService;
        this.modelRouter = modelRouter;
        this.budgetStore = budgetStore;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
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
     * {@link ChatAnswer#degraded} 置真并计入 {@code easyorange.ai.chat.degraded} ——
     * 抛出去只会变成 500 + 通用错误码（调用方读不到「AI 不可用」这个语义，错误率大盘也分不清
     * 供应商故障与代码缺陷），与流式路径的 error 事件口径不一致。预算超限等业务异常照旧上抛，
     * 那是客户端可控的 4xx，不该伪装成降级回答。
     */
    @TokenBudget(scenario = "chat", maxTokensPerCall = 1500, dailyTokenLimit = 300_000)
    public ChatAnswer answer(ChatRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return new ChatAnswer("请描述你的问题", List.of(), request.sessionId(), false);
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

    /**
     * 工具选择计数。「找货类问题占多少」是判断这条 RAG 是落在主链路上还是摆设的第一个数字
     * —— 没有它，检索质量再好也不知道有没有人在用。
     */
    private Counter toolCounter(String tool) {
        return meterRegistry.counter("easyorange.ai.chat.tool", "name", tool);
    }

    private static String staleKey(String question) {
        return AiCallScope.CHAT.cacheKeyPrefix() + question;
    }

    /**
     * 流式回答（SSE）：token 逐段回调；错误统一走 {@link ChatStreamHandler#onError}。
     * <p>
     * 预算记账不在这里做：{@link AiModelSupport} 拿到流末帧的用量分片后按场景记账，
     * {@link #checkBudget()} 只负责前置检查（本方法不带 {@link TokenBudget} 注解，AOP 拦不住，
     * 若两边都记会重复计数）。
     */
    public void streamAnswer(ChatRequest request, ChatStreamHandler handler) {
        if (request.question() == null || request.question().isBlank()) {
            handler.onError("请描述你的问题");
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
        String userId = SecurityContextUtil.getCurrentUserId().orElse(ANONYMOUS_USER);
        List<ChatTurn> history =
                sessionStore.loadRecent(request.sessionId(), aiProperties.chat().historyLimit());
        List<UserPreference> prefs =
                ANONYMOUS_USER.equals(userId) ? List.of() : preferenceRepository.findByUserId(userId);

        // 2. 工具决策（单步 ReAct）：本轮检索什么（知识库 / 在售资产 / 两者）+ 顺带提取用户偏好
        ToolDecision decision = decideTool(request.question(), history, prefs);
        toolCounter(decision.tool()).increment();
        boolean wantKnowledge = TOOL_KNOWLEDGE_SEARCH.equals(decision.tool()) || TOOL_BOTH.equals(decision.tool());
        boolean wantAssets = TOOL_PRODUCT_SEARCH.equals(decision.tool()) || TOOL_BOTH.equals(decision.tool());

        // 3. 执行工具：两条召回是同一形态、不同语料，共用同一份 query
        List<KnowledgeHit> hits = List.of();
        List<AssetHit> assets = List.of();
        if (decision.query() != null && !decision.query().isBlank()) {
            if (wantKnowledge) {
                hits = retrievalService.search(decision.query(), RETRIEVAL_TOP_K);
            }
            if (wantAssets) {
                assets = assetSourcingService.search(decision.query(), ASSET_TOP_K);
            }
        }
        if (decision.preference() != null && !ANONYMOUS_USER.equals(userId)) {
            preferenceRepository.record(
                    userId, decision.preference().key(), decision.preference().value());
        }
        List<String> sources = Stream.concat(
                        hits.stream().map(KnowledgeHit::title), assets.stream().map(AssetHit::title))
                .distinct()
                .limit(3)
                .toList();
        if (handler != null && !sources.isEmpty()) {
            handler.onSources(sources);
        }

        // 4. 生成回答（按角色传消息：system / 历史 user+assistant / 当前 user，流式时逐 token 回调）
        List<Message> messages =
                buildMessages(promptRegistry.require(CHAT_PROMPT), request.question(), history, prefs, hits, assets);
        String answer = handler != null
                ? aiModelSupport.callTextStream(chatModel, AiCallScope.CHAT, messages, handler::onToken)
                : aiModelSupport.callText(chatModel, AiCallScope.CHAT, messages);
        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("AI returned empty answer");
        }

        sessionStore.saveTurn(request.sessionId(), "user", request.question());
        sessionStore.saveTurn(request.sessionId(), "assistant", answer);
        return new ChatAnswer(answer, sources, request.sessionId(), false);
    }

    /**
     * 工具决策：模型输出 JSON 决定是否检索知识库，顺带提取用户偏好。
     * <p>
     * 决策失败（模型故障 / JSON 解析失败）时<b>降级为「直接检索原始问题」</b>而不是不检索：
     * 规则类问题走检索是常态，识别不出来的问题拿原始问句去查一次，最坏是多几条不相关片段进 prompt，
     * 好过把「检索链路失效」伪装成「这题本来就不需要检索」。降级打结构化日志并落 eo_ai_call_log
     * （决策调用本身失败会被记 success=0），不静默。
     */
    private ToolDecision decideTool(String question, List<ChatTurn> history, List<UserPreference> prefs) {
        try {
            String json = aiModelSupport.callJson(
                    modelRouter.choose("chat_tool"),
                    AiCallScope.CHAT,
                    promptRegistry.require(TOOL_PROMPT),
                    buildToolUserMessage(question, history, prefs));
            ToolDecision decision = objectMapper.readValue(json, ToolDecision.class);
            return decision != null ? decision : new ToolDecision("none", question, null);
        } catch (Exception e) {
            log.warn(
                    "action=chat_tool_decision_failed, fallback=knowledge_search, question={}, reason={}",
                    question,
                    e.getMessage());
            return new ToolDecision(TOOL_KNOWLEDGE_SEARCH, question, null);
        }
    }

    private void checkBudget() {
        int used = budgetStore
                .getTodayUsage(CHAT_SCENARIO)
                .map(TokenBudgetStore.TokenUsage::total)
                .orElse(0);
        int maxPerCall = resolveMaxTokensPerCall();
        int dailyLimit = resolveDailyTokenLimit();
        if (dailyLimit > 0 && used + maxPerCall > dailyLimit) {
            // 流式链路绕过 @TokenBudget 切面，这里手动检查 —— 与 TokenBudgetAspect 同一套日志字段
            log.warn(
                    "action=token_budget_exceeded, scenario={}, used={}, maxPerCall={}, limit={}",
                    CHAT_SCENARIO,
                    used,
                    maxPerCall,
                    dailyLimit);
            throw new TokenBudgetExceededException();
        }
    }

    private int resolveMaxTokensPerCall() {
        var cfg = aiProperties.budget().resolve(CHAT_SCENARIO);
        return cfg != null ? cfg.maxTokensPerCall() : DEFAULT_MAX_TOKENS;
    }

    private int resolveDailyTokenLimit() {
        var cfg = aiProperties.budget().resolve(CHAT_SCENARIO);
        return cfg != null ? cfg.dailyTokenLimit() : DEFAULT_DAILY_LIMIT;
    }

    private static String buildToolUserMessage(String question, List<ChatTurn> history, List<UserPreference> prefs) {
        return """
                用户问题：
                <user_question>
                %s
                </user_question>

                历史对话：
                %s

                用户画像：
                %s
                """.formatted(question, formatHistory(history), formatPrefs(prefs));
    }

    /**
     * 组装生成回答的消息序列：system + 历史 user/assistant 轮次 + 当前 user（画像 / 知识片段 / 问题）。
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
            List<KnowledgeHit> hits,
            List<AssetHit> assets) {
        List<Message> messages = new ArrayList<>(history.size() + 2);
        messages.add(new SystemMessage(systemPrompt));
        for (ChatTurn turn : history) {
            messages.add(
                    "user".equals(turn.role())
                            ? new UserMessage(turn.content())
                            : new AssistantMessage(turn.content()));
        }
        messages.add(new UserMessage(buildCurrentUserMessage(question, prefs, hits, assets)));
        return messages;
    }

    private static String buildCurrentUserMessage(
            String question, List<UserPreference> prefs, List<KnowledgeHit> hits, List<AssetHit> assets) {
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
                """.formatted(question, formatPrefs(prefs), formatHits(hits), formatAssets(assets));
    }

    private static String formatHistory(List<ChatTurn> history) {
        if (history.isEmpty()) {
            return "(无)";
        }
        return history.stream()
                .map(turn -> ("user".equals(turn.role()) ? "用户" : "助手") + ": " + turn.content())
                .collect(Collectors.joining("\n"));
    }

    private static String formatPrefs(List<UserPreference> prefs) {
        if (prefs.isEmpty()) {
            return "(无)";
        }
        return prefs.stream().map(p -> p.key() + ": " + p.value()).collect(Collectors.joining("\n"));
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
}
