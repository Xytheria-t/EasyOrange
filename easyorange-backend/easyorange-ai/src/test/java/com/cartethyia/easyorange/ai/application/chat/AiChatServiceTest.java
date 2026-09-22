package com.cartethyia.easyorange.ai.application.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.chat.AgentLoopRunner.Input;
import com.cartethyia.easyorange.ai.application.chat.AgentLoopRunner.Result;
import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.application.dto.ChatRequest;
import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.model.AgentStepView;
import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.port.ChatSessionPort;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import com.cartethyia.easyorange.ai.domain.port.SemanticCachePort;
import com.cartethyia.easyorange.ai.domain.port.UserPreferenceRepository;
import com.cartethyia.easyorange.ai.testsupport.PropertyBindings;
import com.cartethyia.easyorange.ai.testsupport.TestPromptRegistry;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatService (Agent 编排) -> 测试")
class AiChatServiceTest {

    /** 语义缓存的查询向量桩值 — 非空即表示「缓存可用」。 */
    private static final List<Float> QUERY_EMBEDDING = List.of(0.1f, 0.2f);

    @Mock
    private ChatModel chatModel;

    /** 用真实桩而非 mock：{@code PromptRegistry.require} 是接口 default 方法，mock 会把它拦成 null。 */
    private final PromptRegistry promptRegistry = new TestPromptRegistry();

    @Mock
    private AiModelSupport aiModelSupport;

    @Mock
    private SemanticCachePort semanticCache;

    @Mock
    private ChatSessionPort sessionStore;

    @Mock
    private UserPreferenceRepository preferenceRepository;

    @Mock
    private AgentLoopRunner agentLoopRunner;

    private AiProperties aiProperties;
    private Cache<String, Object> staleCache;
    private SimpleMeterRegistry meterRegistry;
    private AiChatService chatService;

    @BeforeEach
    void setUp() {
        aiProperties = PropertyBindings.bind(AiProperties.class);
        staleCache = Caffeine.newBuilder().build();
        meterRegistry = new SimpleMeterRegistry();
        chatService = new AiChatService(
                chatModel,
                promptRegistry,
                aiModelSupport,
                semanticCache,
                sessionStore,
                preferenceRepository,
                agentLoopRunner,
                // 真实实例：默认预算 2000 token，测试历史远小于预算，行为等同直通
                new ChatContextTrimmer(aiProperties, meterRegistry),
                staleCache,
                meterRegistry);
        // 部分用例（空问题/预算超限/缓存命中）不会走到循环，runner 的默认行为允许不被消费
        lenient()
                .when(agentLoopRunner.run(any(Input.class)))
                .thenReturn(new Result(List.of(), List.of(), List.of(), AgentLoopRunner.OUTCOME_FINISHED, 1));
    }

    @Test
    @DisplayName("知识类问题 -> 循环产出知识命中 -> 回答带引用来源")
    void answer_withKnowledgeRetrieval() {
        when(semanticCache.embedQuery(anyString())).thenReturn(QUERY_EMBEDDING);
        when(semanticCache.lookUp(any(), anyString(), anyList(), any())).thenReturn(Optional.empty());
        when(agentLoopRunner.run(any()))
                .thenReturn(new Result(
                        List.of(new KnowledgeHit("kb-0002", "退款规则", "7 天无理由…", 0.95)),
                        List.of(),
                        List.of(),
                        AgentLoopRunner.OUTCOME_FINISHED,
                        2));
        when(aiModelSupport.callText(any(), any(), anyList())).thenReturn("签收后 7 天内支持无理由退货 [来源:退款规则]");

        ChatAnswer answer = chatService.answer(new ChatRequest("怎么退款？", "sess-1", false));

        assertThat(answer.answer()).contains("[来源:退款规则]");
        assertThat(answer.sources()).containsExactly("退款规则");
        // 一轮对话一次写入（提问 + 回答），不留半轮记忆
        verify(sessionStore).saveTurns("sess-1", List.of(ChatTurn.user("怎么退款？"), ChatTurn.assistant(answer.answer())));
        verify(semanticCache).store(any(), anyString(), anyList(), any());
        // 循环输入：问题与会话 ID 透传
        ArgumentCaptor<Input> input = ArgumentCaptor.forClass(Input.class);
        verify(agentLoopRunner).run(input.capture());
        assertThat(input.getValue().question()).isEqualTo("怎么退款？");
        assertThat(input.getValue().sessionId()).isEqualTo("sess-1");
    }

    @Test
    @DisplayName("找货类问题 -> 循环产出资产命中 -> 引用来源为资产，资产块进 prompt")
    @SuppressWarnings("unchecked")
    void answer_withAssetSourcing() {
        when(semanticCache.embedQuery(anyString())).thenReturn(QUERY_EMBEDDING);
        when(semanticCache.lookUp(any(), anyString(), anyList(), any())).thenReturn(Optional.empty());
        when(agentLoopRunner.run(any()))
                .thenReturn(new Result(
                        List.of(),
                        List.of(new AssetHit("p-1", "MacBook Air M1", BigDecimal.valueOf(4200), "数码", "九五新", 0.83)),
                        List.of(),
                        AgentLoopRunner.OUTCOME_FINISHED,
                        2));
        when(aiModelSupport.callText(any(), any(), anyList())).thenReturn("这几件在预算内：MacBook Air M1 [来源:MacBook Air M1]");

        ChatAnswer answer = chatService.answer(new ChatRequest("想找 5000 以内的笔记本", "sess-1", false));

        assertThat(answer.sources()).containsExactly("MacBook Air M1");

        // 循环召回的在售资产必须真的进 prompt（块内形状见 ChatPromptAssemblerTest）
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiModelSupport).callText(any(), any(), captor.capture());
        assertThat(captor.getValue().getLast().getText()).contains("<candidate_assets>", "[p-1]");
    }

    @Test
    @DisplayName("product_detail 轮次的详情 -> <asset_details> 块进 prompt（推荐理由有描述可依）")
    @SuppressWarnings("unchecked")
    void answer_injectsAssetDetails() {
        when(semanticCache.embedQuery(anyString())).thenReturn(QUERY_EMBEDDING);
        when(semanticCache.lookUp(any(), anyString(), anyList(), any())).thenReturn(Optional.empty());
        when(agentLoopRunner.run(any()))
                .thenReturn(new Result(
                        List.of(),
                        List.of(new AssetHit("p-1", "MacBook Air M1", BigDecimal.valueOf(4200), "数码", "九五新", 0.83)),
                        List.of(new AssetDetail(
                                "p-1",
                                "MacBook Air M1",
                                "M1 芯片，95 新无磕碰，电池循环 32 次",
                                BigDecimal.valueOf(4200),
                                "数码",
                                "九五新",
                                "上海",
                                "liming",
                                "ONLINE")),
                        AgentLoopRunner.OUTCOME_FINISHED,
                        3));
        when(aiModelSupport.callText(any(), any(), anyList())).thenReturn("推荐 MacBook [来源:MacBook Air M1]");

        chatService.answer(new ChatRequest("想找 5000 以内的笔记本", "sess-1", false));

        // product_detail 轮次的观察要真的进 prompt（块内形状见 ChatPromptAssemblerTest）
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiModelSupport).callText(any(), any(), captor.capture());
        assertThat(captor.getValue().getLast().getText()).contains("<asset_details>", "[p-1]");
    }

    @Test
    @DisplayName("多来源合并 -> 知识与资产标题去重后进 sources")
    void answer_mergesSourcesFromLoopResult() {
        when(semanticCache.embedQuery(anyString())).thenReturn(QUERY_EMBEDDING);
        when(semanticCache.lookUp(any(), anyString(), anyList(), any())).thenReturn(Optional.empty());
        when(agentLoopRunner.run(any()))
                .thenReturn(new Result(
                        List.of(new KnowledgeHit("kb-0007", "交易规则", "平台担保交易…", 0.9)),
                        List.of(new AssetHit("p-1", "交易规则", null, null, null, 0.5)),
                        List.of(),
                        AgentLoopRunner.OUTCOME_FINISHED,
                        3));
        when(aiModelSupport.callText(any(), any(), anyList())).thenReturn("担保交易保障双方 [来源:交易规则]");

        ChatAnswer answer = chatService.answer(new ChatRequest("5000 的笔记本有吗？平台怎么保障交易？", "sess-1", false));

        assertThat(answer.sources()).containsExactly("交易规则");
    }

    @Test
    @DisplayName("闲聊 -> 循环无召回，直接回答")
    void answer_noTool() {
        when(semanticCache.embedQuery(anyString())).thenReturn(QUERY_EMBEDDING);
        when(semanticCache.lookUp(any(), anyString(), anyList(), any())).thenReturn(Optional.empty());
        when(aiModelSupport.callText(any(), any(), anyList())).thenReturn("在的，有什么可以帮你？");

        ChatAnswer answer = chatService.answer(new ChatRequest("在吗？", "sess-1", false));

        assertThat(answer.answer()).isEqualTo("在的，有什么可以帮你？");
        assertThat(answer.sources()).isEmpty();
    }

    @Test
    @DisplayName("语义缓存命中 -> 不进循环直接返回，且不再写回")
    void answer_cacheHit() {
        ChatAnswer cached = new ChatAnswer("缓存回答", List.of(), "sess-1", false);
        when(semanticCache.embedQuery(anyString())).thenReturn(QUERY_EMBEDDING);
        when(semanticCache.lookUp(any(), anyString(), anyList(), any())).thenReturn(Optional.of(cached));

        ChatAnswer answer = chatService.answer(new ChatRequest("怎么退款？", "sess-1", false));

        assertThat(answer).isEqualTo(cached);
        verify(aiModelSupport, never()).callText(any(), any(), anyList());
        verify(agentLoopRunner, never()).run(any());
        verify(semanticCache, never()).store(any(), anyString(), anyList(), any());
    }

    @Test
    @DisplayName("语义缓存命中 -> sessionId 换成本次请求的（缓存的是回答内容，不是会话身份）")
    void answer_cacheHit_usesCurrentSessionId() {
        when(semanticCache.embedQuery(anyString())).thenReturn(QUERY_EMBEDDING);
        when(semanticCache.lookUp(any(), anyString(), anyList(), any()))
                .thenReturn(Optional.of(new ChatAnswer("缓存回答", List.of("来源A"), "sess-旧", false)));

        ChatAnswer answer = chatService.answer(new ChatRequest("怎么退款？", "sess-新", false));

        assertThat(answer.answer()).isEqualTo("缓存回答");
        assertThat(answer.sources()).containsExactly("来源A");
        assertThat(answer.sessionId()).isEqualTo("sess-新");
    }

    @Test
    @DisplayName("未命中 -> 命中查找与写入共用同一次向量化（不重复 embedding）")
    void answer_cacheMiss_embedsOnce() {
        when(semanticCache.embedQuery(anyString())).thenReturn(QUERY_EMBEDDING);
        when(semanticCache.lookUp(any(), anyString(), anyList(), any())).thenReturn(Optional.empty());
        when(aiModelSupport.callText(any(), any(), anyList())).thenReturn("回答");

        chatService.answer(new ChatRequest("问题", "sess-1", false));

        verify(semanticCache, times(1)).embedQuery(anyString());
        verify(semanticCache).store(any(), anyString(), eq(QUERY_EMBEDDING), any());
    }

    @Test
    @DisplayName("forceFresh -> 跳过语义缓存（评估回归用），连查询向量化都不做")
    void answer_forceFreshSkipsCache() {
        when(aiModelSupport.callText(any(), any(), anyList())).thenReturn("回答");

        chatService.answer(new ChatRequest("问题", "sess-1", true));

        verify(semanticCache, never()).embedQuery(anyString());
        verify(semanticCache, never()).lookUp(any(), anyString(), anyList(), any());
        verify(semanticCache, never()).store(any(), anyString(), anyList(), any());
    }

    @Test
    @DisplayName("流式回答 -> step/token/sources/done 事件依次回调")
    void stream_happyPath() {
        when(agentLoopRunner.run(any())).thenAnswer(invocation -> {
            Input input = invocation.getArgument(0);
            input.handler().onStep(new AgentStepView(1, "knowledge_search", "查退款规则", "命中 1 条"));
            return new Result(
                    List.of(new KnowledgeHit("kb-0002", "退款规则", "7 天无理由…", 0.95)),
                    List.of(),
                    List.of(),
                    AgentLoopRunner.OUTCOME_FINISHED,
                    2);
        });
        when(aiModelSupport.callTextStream(any(), any(), anyList(), any(Consumer.class)))
                .thenAnswer(invocation -> {
                    Consumer<String> consumer = invocation.getArgument(3);
                    consumer.accept("可以");
                    consumer.accept("退款");
                    return "可以退款";
                });

        var tokens = new StringBuilder();
        AtomicReference<List<String>> sources = new AtomicReference<>();
        AtomicReference<String> done = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        List<AgentStepView> steps = new ArrayList<>();
        chatService.streamAnswer(new ChatRequest("怎么退款？", "sess-1", false), new ChatStreamHandler() {
            @Override
            public void onStep(AgentStepView step) {
                steps.add(step);
            }

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
        assertThat(steps).extracting(AgentStepView::tool).containsExactly("knowledge_search");
        assertThat(sources.get()).containsExactly("退款规则");
        assertThat(done.get()).isEqualTo("可以退款");
        assertThat(error.get()).isNull();
    }

    @Test
    @DisplayName("流式回答 -> 预算超限走 onError 降级")
    void stream_budgetExceeded() {
        when(agentLoopRunner.chatBudgetExhausted()).thenReturn(true);

        AtomicReference<String> error = new AtomicReference<>();
        chatService.streamAnswer(new ChatRequest("问题", "sess-1", false), new ChatStreamHandler() {
            @Override
            public void onStep(AgentStepView step) {}

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
        verify(aiModelSupport, never()).callTextStream(any(), any(), anyList(), any());
    }

    @Test
    @DisplayName("空问题 -> onError 提示")
    void stream_blankQuestion() {
        AtomicReference<String> error = new AtomicReference<>();
        chatService.streamAnswer(new ChatRequest("  ", "sess-1", false), new ChatStreamHandler() {
            @Override
            public void onStep(AgentStepView step) {}

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
    @DisplayName("LLM 故障且有缓存旧回答 -> 降级返回旧结果并标记 degraded")
    void answer_llmFailureFallsBackToStale() {
        when(semanticCache.embedQuery(anyString())).thenReturn(QUERY_EMBEDDING);
        when(semanticCache.lookUp(any(), anyString(), anyList(), any())).thenReturn(Optional.empty());
        when(aiModelSupport.callText(any(), any(), anyList()))
                .thenReturn("正常回答")
                .thenThrow(new RuntimeException("DeepSeek 超时"));

        ChatAnswer first = chatService.answer(new ChatRequest("怎么退款？", "sess-1", false));
        ChatAnswer degraded = chatService.answer(new ChatRequest("怎么退款？", "sess-1", false));

        assertThat(first.answer()).isEqualTo("正常回答");
        assertThat(first.degraded()).isFalse();
        assertThat(degraded.answer()).isEqualTo("正常回答");
        assertThat(degraded.degraded()).isTrue();
        verify(aiModelSupport, times(2)).callText(any(), any(), anyList());
    }

    @Test
    @DisplayName("stale 降级 -> sessionId 换成本次请求的，不带出旧会话的 id")
    void answer_staleFallbackUsesCurrentSessionId() {
        when(semanticCache.embedQuery(anyString())).thenReturn(QUERY_EMBEDDING);
        when(semanticCache.lookUp(any(), anyString(), anyList(), any())).thenReturn(Optional.empty());
        when(aiModelSupport.callText(any(), any(), anyList()))
                .thenReturn("正常回答")
                .thenThrow(new RuntimeException("DeepSeek 超时"));

        chatService.answer(new ChatRequest("怎么退款？", "sess-1", false));
        ChatAnswer degraded = chatService.answer(new ChatRequest("怎么退款？", "sess-2", false));

        assertThat(degraded.answer()).isEqualTo("正常回答");
        assertThat(degraded.degraded()).isTrue();
        assertThat(degraded.sessionId()).isEqualTo("sess-2");
    }

    @Test
    @DisplayName("LLM 故障且无缓存 -> 返回降级文案 + degraded 标记（不抛异常，避免落 500）")
    void answer_llmFailureWithoutStaleDegrades() {
        when(semanticCache.embedQuery(anyString())).thenReturn(QUERY_EMBEDDING);
        when(semanticCache.lookUp(any(), anyString(), anyList(), any())).thenReturn(Optional.empty());
        when(aiModelSupport.callText(any(), any(), anyList())).thenThrow(new RuntimeException("DeepSeek 超时"));

        ChatAnswer answer = chatService.answer(new ChatRequest("怎么退款？", "sess-1", false));

        assertThat(answer.answer()).isEqualTo(ChatAnswer.UNAVAILABLE_TEXT);
        assertThat(answer.sources()).isEmpty();
        assertThat(answer.sessionId()).isEqualTo("sess-1");
        assertThat(answer.degraded()).isTrue();
    }

    @Test
    @DisplayName("业务异常（如预算超限）-> 照常上抛，不伪装成降级回答")
    void answer_businessExceptionPropagates() {
        when(semanticCache.embedQuery(anyString())).thenReturn(QUERY_EMBEDDING);
        when(semanticCache.lookUp(any(), anyString(), anyList(), any())).thenReturn(Optional.empty());
        when(aiModelSupport.callText(any(), any(), anyList())).thenThrow(BusinessException.of("AI 调用预算已用尽"));

        Assertions.assertThatThrownBy(() -> chatService.answer(new ChatRequest("怎么退款？", "sess-1", false)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 调用预算已用尽");
    }

    @Test
    @DisplayName("forceFresh 成功回答同样写入降级缓存（后续故障可兜底）")
    void answer_forceFreshWritesStaleCache() {
        when(aiModelSupport.callText(any(), any(), anyList()))
                .thenReturn("新鲜回答")
                .thenThrow(new RuntimeException("DeepSeek 超时"));

        ChatAnswer first = chatService.answer(new ChatRequest("问题", "sess-1", true));
        ChatAnswer degraded = chatService.answer(new ChatRequest("问题", "sess-1", true));

        assertThat(first.answer()).isEqualTo("新鲜回答");
        assertThat(degraded.answer()).isEqualTo("新鲜回答");
        assertThat(degraded.degraded()).isTrue();
    }

    @Test
    @DisplayName("历史记忆注入 -> 按角色分发为历史消息，不压平进当前 user 消息")
    void answer_injectsHistory() {
        when(semanticCache.embedQuery(anyString())).thenReturn(QUERY_EMBEDDING);
        when(semanticCache.lookUp(any(), anyString(), anyList(), any())).thenReturn(Optional.empty());
        when(sessionStore.loadRecent("sess-1"))
                .thenReturn(List.of(ChatTurn.user("上一轮问题"), ChatTurn.assistant("上一轮回答")));
        when(aiModelSupport.callText(any(), any(), anyList())).thenAnswer(invocation -> {
            List<Message> messages = invocation.getArgument(2);
            assertThat(messages).hasSize(4);
            assertThat(messages.getFirst().getMessageType()).isEqualTo(MessageType.SYSTEM);
            assertThat(messages.get(1).getMessageType()).isEqualTo(MessageType.USER);
            assertThat(messages.get(1).getText()).isEqualTo("上一轮问题");
            assertThat(messages.get(2).getMessageType()).isEqualTo(MessageType.ASSISTANT);
            assertThat(messages.get(2).getText()).isEqualTo("上一轮回答");
            // 历史不进当前 user 消息：前缀稳定才命中供应商上下文缓存（重复前缀按折扣计价）
            assertThat(messages.get(3).getText()).contains("<user_question>").contains("继续");
            assertThat(messages.get(3).getText()).doesNotContain("上一轮问题");
            return "记住了";
        });

        ChatAnswer answer = chatService.answer(new ChatRequest("继续", "sess-1", false));

        assertThat(answer.answer()).isEqualTo("记住了");
    }
}
