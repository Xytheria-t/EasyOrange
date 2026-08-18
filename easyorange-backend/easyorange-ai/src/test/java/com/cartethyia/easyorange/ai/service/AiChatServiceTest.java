package com.cartethyia.easyorange.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.adapter.outbound.cache.ChatSessionStore;
import com.cartethyia.easyorange.ai.adapter.outbound.cache.SemanticCacheService;
import com.cartethyia.easyorange.ai.budget.TokenBudgetStore;
import com.cartethyia.easyorange.ai.chat.ChatStreamHandler;
import com.cartethyia.easyorange.ai.chat.ChatTurn;
import com.cartethyia.easyorange.ai.chat.UserPreferenceRepository;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.dto.ChatRequest;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeHit;
import com.cartethyia.easyorange.ai.prompt.PromptRegistry;
import com.cartethyia.easyorange.ai.prompt.PromptTemplate;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatService (Agent 编排) -> 测试")
class AiChatServiceTest {

    private static final String TOOL_TEMPLATE = "你是工具决策器";
    private static final String CHAT_TEMPLATE = "你是 EasyOrange AI 助手";

    @Mock
    private ChatModel chatModel;

    @Mock
    private PromptRegistry promptRegistry;

    @Mock
    private AiModelSupport aiModelSupport;

    @Mock
    private SemanticCacheService semanticCache;

    @Mock
    private ChatSessionStore sessionStore;

    @Mock
    private UserPreferenceRepository preferenceRepository;

    @Mock
    private KnowledgeRetrievalService retrievalService;

    @Mock
    private AiModelRouter modelRouter;

    @Mock
    private TokenBudgetStore budgetStore;

    private AiProperties aiProperties;
    private Cache<String, Object> staleCache;
    private AiChatService chatService;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        staleCache = Caffeine.newBuilder().build();
        chatService = new AiChatService(
                chatModel,
                promptRegistry,
                aiModelSupport,
                semanticCache,
                sessionStore,
                preferenceRepository,
                retrievalService,
                modelRouter,
                budgetStore,
                aiProperties,
                new ObjectMapper(),
                staleCache);
        // 部分用例（空问题/预算超限/缓存命中）不会走到工具决策，prompt/router stub 允许不被消费
        lenient()
                .when(promptRegistry.getLatest("ai_chat_tool_system"))
                .thenReturn(Optional.of(new PromptTemplate("ai_chat_tool_system", "v1", TOOL_TEMPLATE, "tool")));
        lenient()
                .when(promptRegistry.getLatest("ai_chat_system"))
                .thenReturn(Optional.of(new PromptTemplate("ai_chat_system", "v1", CHAT_TEMPLATE, "chat")));
        lenient().when(modelRouter.choose("chat_tool")).thenReturn(chatModel);
    }

    @Test
    @DisplayName("知识类问题 -> 工具决策命中知识库检索 -> 回答带引用来源")
    void answer_withKnowledgeRetrieval() {
        when(semanticCache.get(any(), anyString(), any())).thenReturn(Optional.empty());
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn("{\"tool\":\"knowledge_search\",\"query\":\"退款\",\"preference\":null}");
        when(aiModelSupport.callText(any(), any(), anyString(), anyString())).thenReturn("签收后 7 天内支持无理由退货 [来源:退款规则]");
        when(retrievalService.search("退款", 5))
                .thenReturn(List.of(new KnowledgeHit("kb-0002", "退款规则", "7 天无理由…", 0.95)));

        ChatAnswer answer = chatService.answer(new ChatRequest("怎么退款？", "sess-1", false));

        assertThat(answer.answer()).contains("[来源:退款规则]");
        assertThat(answer.sources()).containsExactly("退款规则");
        verify(retrievalService).search("退款", 5);
        verify(sessionStore).saveTurn("sess-1", "user", "怎么退款？");
        verify(sessionStore).saveTurn("sess-1", "assistant", answer.answer());
        verify(semanticCache).put(any(), anyString(), any());
    }

    @Test
    @DisplayName("闲聊 -> 不触发检索，直接回答")
    void answer_noTool() {
        when(semanticCache.get(any(), anyString(), any())).thenReturn(Optional.empty());
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn("{\"tool\":\"none\",\"query\":\"\",\"preference\":null}");
        when(aiModelSupport.callText(any(), any(), anyString(), anyString())).thenReturn("在的，有什么可以帮你？");

        ChatAnswer answer = chatService.answer(new ChatRequest("在吗？", "sess-1", false));

        assertThat(answer.answer()).isEqualTo("在的，有什么可以帮你？");
        assertThat(answer.sources()).isEmpty();
        verify(retrievalService, never()).search(anyString(), any(Integer.class));
    }

    @Test
    @DisplayName("对话中出现偏好 -> 提取并写入用户画像（长期记忆）")
    void answer_extractsPreference() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(new AuthUser("user-1", "tester"), null, List.of()));
        assertThat(SecurityContextUtil.getCurrentUserId()).contains("user-1");
        when(semanticCache.get(any(), anyString(), any())).thenReturn(Optional.empty());
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn("{\"tool\":\"none\",\"query\":\"\",\"preference\":{\"key\":\"style\",\"value\":\"复古\"}}");
        when(aiModelSupport.callText(any(), any(), anyString(), anyString())).thenReturn("好的，记住你喜欢复古风格。");

        chatService.answer(new ChatRequest("我喜欢复古风格的东西", "sess-1", false));

        verify(preferenceRepository).record("user-1", "style", "复古");
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("语义缓存命中 -> 不调模型直接返回")
    void answer_cacheHit() {
        ChatAnswer cached = new ChatAnswer("缓存回答", List.of(), "sess-1");
        when(semanticCache.get(any(), anyString(), any())).thenReturn(Optional.of(cached));

        ChatAnswer answer = chatService.answer(new ChatRequest("怎么退款？", "sess-1", false));

        assertThat(answer).isEqualTo(cached);
        verify(aiModelSupport, never()).callText(any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("forceFresh -> 跳过语义缓存（评估回归用）")
    void answer_forceFreshSkipsCache() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn("{\"tool\":\"none\",\"query\":\"\",\"preference\":null}");
        when(aiModelSupport.callText(any(), any(), anyString(), anyString())).thenReturn("回答");

        chatService.answer(new ChatRequest("问题", "sess-1", true));

        verify(semanticCache, never()).get(any(), anyString(), any());
        verify(semanticCache, never()).put(any(), anyString(), any());
    }

    @Test
    @DisplayName("流式回答 -> token/sources/done 事件依次回调")
    void stream_happyPath() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn("{\"tool\":\"knowledge_search\",\"query\":\"退款\",\"preference\":null}");
        when(retrievalService.search("退款", 5))
                .thenReturn(List.of(new KnowledgeHit("kb-0002", "退款规则", "7 天无理由…", 0.95)));
        when(aiModelSupport.callTextStream(any(), any(), anyString(), anyString(), any(Consumer.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Consumer<String> consumer = invocation.getArgument(4);
                    consumer.accept("可以");
                    consumer.accept("退款");
                    return "可以退款";
                });
        when(budgetStore.getTodayUsage("chat")).thenReturn(Optional.of(new TokenBudgetStore.TokenUsage(10, 10, 0)));

        var tokens = new StringBuilder();
        AtomicReference<List<String>> sources = new AtomicReference<>();
        AtomicReference<String> done = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        chatService.streamAnswer(new ChatRequest("怎么退款？", "sess-1", false), new ChatStreamHandler() {
            @Override
            public void onToken(String token) {
                tokens.append(token);
            }

            @Override
            public void onSources(List<String> src) {
                sources.set(src);
            }

            @Override
            public void onDone(String fullAnswer) {
                done.set(fullAnswer);
            }

            @Override
            public void onError(String message) {
                error.set(message);
            }
        });

        assertThat(tokens.toString()).isEqualTo("可以退款");
        assertThat(sources.get()).containsExactly("退款规则");
        assertThat(done.get()).isEqualTo("可以退款");
        assertThat(error.get()).isNull();
        verify(budgetStore).recordUsage(eq("chat"), any(Integer.class), eq(0));
    }

    @Test
    @DisplayName("流式回答 -> 预算超限走 onError 降级")
    void stream_budgetExceeded() {
        when(budgetStore.getTodayUsage("chat")).thenReturn(Optional.of(new TokenBudgetStore.TokenUsage(500_000, 0, 0)));

        AtomicReference<String> error = new AtomicReference<>();
        chatService.streamAnswer(new ChatRequest("问题", "sess-1", false), new ChatStreamHandler() {
            @Override
            public void onToken(String token) {}

            @Override
            public void onSources(List<String> sources) {}

            @Override
            public void onDone(String fullAnswer) {}

            @Override
            public void onError(String message) {
                error.set(message);
            }
        });

        assertThat(error.get()).contains("预算");
        verify(aiModelSupport, never()).callTextStream(any(), any(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("空问题 -> onError 提示")
    void stream_blankQuestion() {
        AtomicReference<String> error = new AtomicReference<>();
        chatService.streamAnswer(new ChatRequest("  ", "sess-1", false), new ChatStreamHandler() {
            @Override
            public void onToken(String token) {}

            @Override
            public void onSources(List<String> sources) {}

            @Override
            public void onDone(String fullAnswer) {}

            @Override
            public void onError(String message) {
                error.set(message);
            }
        });

        assertThat(error.get()).contains("问题");
    }

    @Test
    @DisplayName("LLM 故障且有缓存旧回答 -> 降级返回旧结果")
    void answer_llmFailureFallsBackToStale() {
        when(semanticCache.get(any(), anyString(), any())).thenReturn(Optional.empty());
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn("{\"tool\":\"none\",\"query\":\"\",\"preference\":null}");
        when(aiModelSupport.callText(any(), any(), anyString(), anyString()))
                .thenReturn("正常回答")
                .thenThrow(new RuntimeException("DeepSeek 超时"));

        ChatAnswer first = chatService.answer(new ChatRequest("怎么退款？", "sess-1", false));
        ChatAnswer degraded = chatService.answer(new ChatRequest("怎么退款？", "sess-1", false));

        assertThat(first.answer()).isEqualTo("正常回答");
        assertThat(degraded.answer()).isEqualTo("正常回答");
        verify(aiModelSupport, times(2)).callText(any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("LLM 故障且无缓存 -> 异常上抛（不做降级）")
    void answer_llmFailureWithoutStaleRethrows() {
        when(semanticCache.get(any(), anyString(), any())).thenReturn(Optional.empty());
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn("{\"tool\":\"none\",\"query\":\"\",\"preference\":null}");
        when(aiModelSupport.callText(any(), any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("DeepSeek 超时"));

        Assertions.assertThatThrownBy(() -> chatService.answer(new ChatRequest("怎么退款？", "sess-1", false)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DeepSeek 超时");
    }

    @Test
    @DisplayName("forceFresh 成功回答同样写入降级缓存（后续故障可兜底）")
    void answer_forceFreshWritesStaleCache() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn("{\"tool\":\"none\",\"query\":\"\",\"preference\":null}");
        when(aiModelSupport.callText(any(), any(), anyString(), anyString()))
                .thenReturn("新鲜回答")
                .thenThrow(new RuntimeException("DeepSeek 超时"));

        ChatAnswer first = chatService.answer(new ChatRequest("问题", "sess-1", true));
        ChatAnswer degraded = chatService.answer(new ChatRequest("问题", "sess-1", true));

        assertThat(first.answer()).isEqualTo("新鲜回答");
        assertThat(degraded.answer()).isEqualTo("新鲜回答");
    }

    @Test
    @DisplayName("历史记忆注入 -> 组装 user 消息时携带最近轮次")
    void answer_injectsHistory() {
        when(semanticCache.get(any(), anyString(), any())).thenReturn(Optional.empty());
        when(sessionStore.loadRecent("sess-1", 6))
                .thenReturn(List.of(new ChatTurn("user", "上一轮问题"), new ChatTurn("assistant", "上一轮回答")));
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn("{\"tool\":\"none\",\"query\":\"\",\"preference\":null}");
        when(aiModelSupport.callText(any(), any(), anyString(), anyString())).thenAnswer(invocation -> {
            String userMessage = invocation.getArgument(3);
            assertThat(userMessage).contains("上一轮问题").contains("上一轮回答");
            return "记住了";
        });

        ChatAnswer answer = chatService.answer(new ChatRequest("继续", "sess-1", false));

        assertThat(answer.answer()).isEqualTo("记住了");
    }
}
