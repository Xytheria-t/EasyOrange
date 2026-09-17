package com.cartethyia.easyorange.ai.application.service;

import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.application.dto.ChatRequest;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.exception.TokenBudgetExceededException;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.PromptTemplate;
import com.cartethyia.easyorange.ai.domain.model.ToolDecision;
import com.cartethyia.easyorange.ai.domain.model.UserPreference;
import com.cartethyia.easyorange.ai.domain.port.ChatSessionPort;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import com.cartethyia.easyorange.ai.domain.port.SemanticCachePort;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStore;
import com.cartethyia.easyorange.ai.domain.port.UserPreferenceRepository;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
 * 2. 工具决策：模型输出 JSON 决定是否检索知识库（knowledge_search），顺带提取用户偏好
 * 3. 执行工具：KnowledgeRetrievalService 混合召回 + Cosine 重排，返回带来源的命中
 * 4. 生成回答：system prompt 注入画像/历史/检索结果，回答末尾 [来源:标题] 引用溯源
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
    private static final String TOOL_KNOWLEDGE_SEARCH = "knowledge_search";
    private static final String ANONYMOUS_USER = "anonymous";

    private final ChatModel chatModel;
    private final PromptRegistry promptRegistry;
    private final AiModelSupport aiModelSupport;
    private final SemanticCachePort semanticCache;
    private final ChatSessionPort sessionStore;
    private final UserPreferenceRepository preferenceRepository;
    private final KnowledgeRetrievalService retrievalService;
    private final AiModelRouter modelRouter;
    private final TokenBudgetStore budgetStore;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final Cache<String, Object> staleCache;

    // Lombok 构造器不会把 @Qualifier 复制到参数上，故手写显式构造器以保留 "aiStaleCache" 限定
    public AiChatService(
            ChatModel chatModel,
            PromptRegistry promptRegistry,
            AiModelSupport aiModelSupport,
            SemanticCachePort semanticCache,
            ChatSessionPort sessionStore,
            UserPreferenceRepository preferenceRepository,
            KnowledgeRetrievalService retrievalService,
            AiModelRouter modelRouter,
            TokenBudgetStore budgetStore,
            AiProperties aiProperties,
            ObjectMapper objectMapper,
            @Qualifier("aiStaleCache") Cache<String, Object> staleCache) {
        this.chatModel = chatModel;
        this.promptRegistry = promptRegistry;
        this.aiModelSupport = aiModelSupport;
        this.semanticCache = semanticCache;
        this.sessionStore = sessionStore;
        this.preferenceRepository = preferenceRepository;
        this.retrievalService = retrievalService;
        this.modelRouter = modelRouter;
        this.budgetStore = budgetStore;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.staleCache = staleCache;
    }

    /**
     * 非流式回答（语义缓存 + 预算 AOP + 故障降级）。
     * <p>
     * 降级语义：LLM 调用失败（供应商超时/异常）时返回 24h 窗口内的缓存旧回答，
     * 保证问答可用性；缓存随每次成功回答无条件刷新（forceFresh 场景同样可兜底）。
     */
    @TokenBudget(scenario = "chat", maxTokensPerCall = 1500, dailyTokenLimit = 300_000)
    public ChatAnswer answer(ChatRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return new ChatAnswer("请描述你的问题", List.of(), request.sessionId());
        }
        try {
            if (!request.forceFresh()) {
                var cached = semanticCache.get(AiCallScope.CHAT, request.question(), ChatAnswer.class);
                if (cached.isPresent()) {
                    return cached.get();
                }
            }
            ChatAnswer answer = agenticAnswer(request, null);
            if (!request.forceFresh()) {
                semanticCache.put(AiCallScope.CHAT, request.question(), answer);
            }
            staleCache.put(staleKey(request.question()), answer);
            return answer;
        } catch (Exception e) {
            Object stale = staleCache.getIfPresent(staleKey(request.question()));
            if (stale instanceof ChatAnswer degraded) {
                log.warn("LLM 调用失败，降级返回缓存旧回答: {}", e.getMessage());
                return degraded;
            }
            throw e;
        }
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
            log.error("chat stream failed, question={}", request.question(), e);
            handler.onError("AI 服务暂时不可用，请稍后重试");
        }
    }

    private ChatAnswer agenticAnswer(ChatRequest request, @Nullable ChatStreamHandler handler) {
        String userId = SecurityContextUtil.getCurrentUserId().orElse(ANONYMOUS_USER);
        List<ChatTurn> history =
                sessionStore.loadRecent(request.sessionId(), aiProperties.chat().historyLimit());
        List<UserPreference> prefs =
                ANONYMOUS_USER.equals(userId) ? List.of() : preferenceRepository.findByUserId(userId);

        // 2. 工具决策（单步 ReAct）：是否检索知识库 + 顺带提取用户偏好
        ToolDecision decision = decideTool(request.question(), history, prefs);
        List<KnowledgeHit> hits = List.of();
        if (TOOL_KNOWLEDGE_SEARCH.equals(decision.tool())
                && decision.query() != null
                && !decision.query().isBlank()) {
            hits = retrievalService.search(decision.query(), RETRIEVAL_TOP_K);
        }
        if (decision.preference() != null && !ANONYMOUS_USER.equals(userId)) {
            preferenceRepository.record(
                    userId, decision.preference().key(), decision.preference().value());
        }
        List<String> sources =
                hits.stream().map(KnowledgeHit::title).distinct().limit(3).toList();
        if (handler != null && !sources.isEmpty()) {
            handler.onSources(sources);
        }

        // 4. 生成回答（按角色传消息：system / 历史 user+assistant / 当前 user，流式时逐 token 回调）
        List<Message> messages = buildMessages(loadSystemPrompt(CHAT_PROMPT), request.question(), history, prefs, hits);
        String answer = handler != null
                ? aiModelSupport.callTextStream(chatModel, AiCallScope.CHAT, messages, handler::onToken)
                : aiModelSupport.callText(chatModel, AiCallScope.CHAT, messages);
        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("AI returned empty answer");
        }

        sessionStore.saveTurn(request.sessionId(), "user", request.question());
        sessionStore.saveTurn(request.sessionId(), "assistant", answer);
        return new ChatAnswer(answer, sources, request.sessionId());
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
                    loadSystemPrompt(TOOL_PROMPT),
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

    private String loadSystemPrompt(String name) {
        return promptRegistry
                .getLatest(name)
                .map(PromptTemplate::template)
                .orElseThrow(() -> new IllegalStateException("Prompt template not found: " + name));
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
            List<KnowledgeHit> hits) {
        List<Message> messages = new ArrayList<>(history.size() + 2);
        messages.add(new SystemMessage(systemPrompt));
        for (ChatTurn turn : history) {
            messages.add(
                    "user".equals(turn.role())
                            ? new UserMessage(turn.content())
                            : new AssistantMessage(turn.content()));
        }
        messages.add(new UserMessage(buildCurrentUserMessage(question, prefs, hits)));
        return messages;
    }

    private static String buildCurrentUserMessage(
            String question, List<UserPreference> prefs, List<KnowledgeHit> hits) {
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
                """.formatted(question, formatPrefs(prefs), formatHits(hits));
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
}
