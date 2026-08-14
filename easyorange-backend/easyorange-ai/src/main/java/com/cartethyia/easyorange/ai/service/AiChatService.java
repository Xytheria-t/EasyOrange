package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.adapter.outbound.cache.ChatSessionStore;
import com.cartethyia.easyorange.ai.adapter.outbound.cache.SemanticCacheService;
import com.cartethyia.easyorange.ai.budget.TokenBudget;
import com.cartethyia.easyorange.ai.budget.TokenBudgetExceededException;
import com.cartethyia.easyorange.ai.budget.TokenBudgetStore;
import com.cartethyia.easyorange.ai.chat.ChatStreamHandler;
import com.cartethyia.easyorange.ai.chat.ChatTurn;
import com.cartethyia.easyorange.ai.chat.ToolDecision;
import com.cartethyia.easyorange.ai.chat.UserPreference;
import com.cartethyia.easyorange.ai.chat.UserPreferenceRepository;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.dto.ChatRequest;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeHit;
import com.cartethyia.easyorange.ai.prompt.PromptRegistry;
import com.cartethyia.easyorange.ai.prompt.PromptTemplate;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ChatModel;
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
@RequiredArgsConstructor
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
    private final SemanticCacheService semanticCache;
    private final ChatSessionStore sessionStore;
    private final UserPreferenceRepository preferenceRepository;
    private final KnowledgeRetrievalService retrievalService;
    private final AiModelRouter modelRouter;
    private final TokenBudgetStore budgetStore;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    /**
     * 非流式回答（语义缓存 + 预算 AOP）。
     */
    @TokenBudget(scenario = "chat", maxTokensPerCall = 1500, dailyTokenLimit = 300_000)
    public ChatAnswer answer(ChatRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return new ChatAnswer("请描述你的问题", List.of(), request.sessionId());
        }
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
        return answer;
    }

    /**
     * 流式回答（SSE）：token 逐段回调；错误统一走 {@link ChatStreamHandler#onError}。
     */
    public void streamAnswer(ChatRequest request, ChatStreamHandler handler) {
        if (request.question() == null || request.question().isBlank()) {
            handler.onError("请描述你的问题");
            return;
        }
        try {
            checkBudget();
            ChatAnswer answer = agenticAnswer(request, handler);
            budgetStore.recordUsage(CHAT_SCENARIO, resolveMaxTokensPerCall(), 0);
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
        List<ChatTurn> history = sessionStore.loadRecent(
                request.sessionId(), aiProperties.getChat().getHistoryLimit());
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

        // 4. 生成回答（流式时逐 token 回调）
        String systemPrompt = loadSystemPrompt(CHAT_PROMPT);
        String userMessage = buildUserMessage(request.question(), history, prefs, hits);
        String answer = handler != null
                ? aiModelSupport.callTextStream(
                        chatModel, AiCallScope.CHAT, systemPrompt, userMessage, handler::onToken)
                : aiModelSupport.callText(chatModel, AiCallScope.CHAT, systemPrompt, userMessage);
        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("AI returned empty answer");
        }

        sessionStore.saveTurn(request.sessionId(), "user", request.question());
        sessionStore.saveTurn(request.sessionId(), "assistant", answer);
        return new ChatAnswer(answer, sources, request.sessionId());
    }

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
            log.warn("tool decision failed, fallback to direct answer: {}", e.getMessage());
            return new ToolDecision("none", question, null);
        }
    }

    private void checkBudget() {
        int used = budgetStore
                .getTodayUsage(CHAT_SCENARIO)
                .map(TokenBudgetStore.TokenUsage::total)
                .orElse(0);
        int dailyLimit = resolveDailyTokenLimit();
        if (dailyLimit > 0 && used + resolveMaxTokensPerCall() > dailyLimit) {
            throw new TokenBudgetExceededException(CHAT_SCENARIO, used, dailyLimit);
        }
    }

    private int resolveMaxTokensPerCall() {
        var cfg = aiProperties.getBudget().resolve(CHAT_SCENARIO);
        return cfg != null ? cfg.getMaxTokensPerCall() : DEFAULT_MAX_TOKENS;
    }

    private int resolveDailyTokenLimit() {
        var cfg = aiProperties.getBudget().resolve(CHAT_SCENARIO);
        return cfg != null ? cfg.getDailyTokenLimit() : DEFAULT_DAILY_LIMIT;
    }

    private String loadSystemPrompt(String name) {
        return promptRegistry
                .getLatest(name)
                .map(PromptTemplate::template)
                .orElseThrow(() -> new IllegalStateException("Prompt template not found: " + name));
    }

    private static String buildToolUserMessage(String question, List<ChatTurn> history, List<UserPreference> prefs) {
        return """
                用户问题：%s

                历史对话：
                %s

                用户画像：
                %s
                """.formatted(question, formatHistory(history), formatPrefs(prefs));
    }

    private static String buildUserMessage(
            String question, List<ChatTurn> history, List<UserPreference> prefs, List<KnowledgeHit> hits) {
        return """
                用户问题：%s

                历史对话：
                %s

                用户画像：
                %s

                知识库检索结果：
                %s
                """.formatted(question, formatHistory(history), formatPrefs(prefs), formatHits(hits));
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
