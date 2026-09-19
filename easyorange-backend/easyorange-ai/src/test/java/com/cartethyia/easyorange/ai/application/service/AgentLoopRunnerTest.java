package com.cartethyia.easyorange.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.service.AgentLoopRunner.Input;
import com.cartethyia.easyorange.ai.application.service.AgentLoopRunner.Result;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.model.AgentStepTrace;
import com.cartethyia.easyorange.ai.domain.model.AgentStepView;
import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.port.AgentTracePort;
import com.cartethyia.easyorange.ai.domain.port.AssetDetailPort;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistry;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStore;
import com.cartethyia.easyorange.ai.domain.port.UserPreferenceRepository;
import com.cartethyia.easyorange.ai.testsupport.PropertyBindings;
import com.cartethyia.easyorange.ai.testsupport.TestPromptRegistry;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentLoopRunner (多步 ReAct 循环) -> 测试")
class AgentLoopRunnerTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private AiModelSupport aiModelSupport;

    @Mock
    private AiModelRouter modelRouter;

    /** 用真实桩而非 mock：{@code PromptRegistry.require} 是接口 default 方法，mock 会把它拦成 null。 */
    private final PromptRegistry promptRegistry = new TestPromptRegistry();

    @Mock
    private KnowledgeRetrievalService retrievalService;

    @Mock
    private AssetSourcingService assetSourcingService;

    @Mock
    private AssetDetailPort assetDetailPort;

    @Mock
    private AgentTracePort tracePort;

    @Mock
    private UserPreferenceRepository preferenceRepository;

    @Mock
    private TokenBudgetStore budgetStore;

    @Mock
    private IdGenerator idGenerator;

    private AiProperties aiProperties;
    private SimpleMeterRegistry meterRegistry;
    private AgentLoopRunner runner;

    @BeforeEach
    void setUp() {
        aiProperties = PropertyBindings.bind(AiProperties.class);
        meterRegistry = new SimpleMeterRegistry();
        runner = new AgentLoopRunner(
                aiModelSupport,
                modelRouter,
                promptRegistry,
                retrievalService,
                assetSourcingService,
                assetDetailPort,
                tracePort,
                preferenceRepository,
                budgetStore,
                aiProperties,
                new ObjectMapper(),
                idGenerator,
                meterRegistry);
        lenient().when(modelRouter.choose("chat_tool")).thenReturn(chatModel);
        lenient().when(idGenerator.generateId()).thenReturn("trace-1");
        lenient().when(budgetStore.getTodayUsage("chat")).thenReturn(Optional.empty());
    }

    /** 决策器输出的合法 JSON（模型侧契约）。 */
    private static String decision(String tool, String query) {
        return "{\"thought\":\"查一下\",\"tool\":\"%s\",\"query\":\"%s\",\"productId\":null,\"preference\":null}"
                .formatted(tool, query);
    }

    private static String detailDecision(String productId) {
        return "{\"thought\":\"看详情\",\"tool\":\"product_detail\",\"query\":\"\",\"productId\":\"%s\",\"preference\":null}"
                .formatted(productId);
    }

    private Result run(String question, String userId, ChatStreamHandler handler) {
        return runner.run(new Input(question, "sess-1", userId, List.of(), List.of(), handler));
    }

    private Result run(String question) {
        return run(question, "anonymous", null);
    }

    @Test
    @DisplayName("首轮 finish（寒暄）-> 不调任何工具，trace 落 finish 行，step 事件推送")
    void run_finishOnFirstRound() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn(
                        "{\"thought\":\"闲聊无需检索\",\"tool\":\"finish\",\"query\":\"\",\"productId\":null,\"preference\":null}");
        var steps = new RecordingHandler();

        Result result = run("在吗？", "anonymous", steps);

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        assertThat(result.rounds()).isEqualTo(1);
        assertThat(result.knowledgeHits()).isEmpty();
        verifyNoInteractions(retrievalService, assetSourcingService);
        verify(tracePort)
                .record(argThat(trace ->
                        "finish".equals(trace.tool()) && trace.stepIndex() == 1 && "trace-1".equals(trace.traceId())));
        assertThat(steps.steps).hasSize(1);
        assertThat(steps.steps.getFirst().tool()).isEqualTo("finish");
        assertThat(steps.steps.getFirst().thought()).isEqualTo("闲聊无需检索");
        assertThat(meterRegistry
                        .counter("easyorange.ai.chat.loop", "outcome", "finished")
                        .count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.get("easyorange.ai.chat.steps").summary().count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("两步循环：知识检索 -> finish -> 检索执行一次，观察进入下一轮决策上下文")
    void run_twoRoundsWithKnowledgeSearch() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn(decision("knowledge_search", "退款"), decision("finish", ""));
        when(retrievalService.search("退款", 5))
                .thenReturn(List.of(new KnowledgeHit("kb-0002", "退款规则", "7 天无理由…", 0.95)));

        Result result = run("怎么退款？");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        assertThat(result.rounds()).isEqualTo(2);
        assertThat(result.knowledgeHits()).hasSize(1);
        verify(retrievalService).search("退款", 5);
        verify(tracePort, times(2)).record(any(AgentStepTrace.class));

        // 第二轮决策的 user message 携带第一步的工具与观察（ReAct 的核心：观察驱动下一步）
        ArgumentCaptor<String> userMessage = ArgumentCaptor.forClass(String.class);
        verify(aiModelSupport, times(2)).callJson(any(), any(), anyString(), userMessage.capture());
        assertThat(userMessage.getAllValues().get(1)).contains("第 1 步 [knowledge_search] 退款", "观察：命中 1 条：退款规则");
    }

    @Test
    @DisplayName("三步循环：product_search -> product_detail -> finish，观察带资产 ID、详情进 Result")
    void run_productDetailFlow() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn(decision("product_search", "5000 笔记本"), detailDecision("p-1"), decision("finish", ""));
        when(assetSourcingService.search("5000 笔记本", 5))
                .thenReturn(
                        List.of(new AssetHit("p-1", "MacBook Air M1", BigDecimal.valueOf(4200), "数码", "九五新", 0.83)));
        when(assetDetailPort.findDetail("p-1"))
                .thenReturn(Optional.of(new AssetDetail(
                        "p-1",
                        "MacBook Air M1",
                        "M1 芯片，95 新无磕碰",
                        BigDecimal.valueOf(4200),
                        "数码",
                        "九五新",
                        "上海",
                        "liming",
                        "ONLINE")));
        var steps = new RecordingHandler();

        Result result = run("预算 5000 想买笔记本", "user-1", steps);

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        assertThat(result.rounds()).isEqualTo(3);
        assertThat(result.assets()).hasSize(1);
        assertThat(result.details()).hasSize(1);
        assertThat(result.details().getFirst().description()).isEqualTo("M1 芯片，95 新无磕碰");

        // 步骤事件序列完整（前端步骤可视化的数据源）
        assertThat(steps.steps)
                .extracting(AgentStepView::tool)
                .containsExactly(
                        AgentLoopRunner.TOOL_PRODUCT_SEARCH,
                        AgentLoopRunner.TOOL_PRODUCT_DETAIL,
                        AgentLoopRunner.TOOL_FINISH);

        // 召回观察带 [资产 ID]（product_detail 的 productId 取值锚点）与价格
        ArgumentCaptor<String> userMessage = ArgumentCaptor.forClass(String.class);
        verify(aiModelSupport, times(3)).callJson(any(), any(), anyString(), userMessage.capture());
        assertThat(userMessage.getAllValues().get(1)).contains("[p-1] MacBook Air M1 ¥4200");
        assertThat(userMessage.getAllValues().get(2)).contains("第 2 步 [product_detail] p-1", "M1 芯片，95 新无磕碰");

        // 详情步骤的 trace 带入参 productId
        verify(tracePort, times(3))
                .record(argThat(trace ->
                        !AgentLoopRunner.TOOL_PRODUCT_DETAIL.equals(trace.tool()) || "p-1".equals(trace.toolInput())));
    }

    @Test
    @DisplayName("步数上限内未 finish -> 强制收敛（step_limit），不再发第 N+1 次决策")
    void run_stepLimitForcesConvergence() {
        aiProperties = PropertyBindings.bind(AiProperties.class, "chat.max-steps", "2");
        runner = newRunner();
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn(
                        decision("knowledge_search", "退款"),
                        decision("knowledge_search", "退货"),
                        decision("knowledge_search", "换货"));
        when(retrievalService.search(anyString(), anyInt())).thenReturn(List.of());

        Result result = run("反复问规则");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_STEP_LIMIT);
        assertThat(result.rounds()).isEqualTo(2);
        verify(aiModelSupport, times(2)).callJson(any(), any(), anyString(), anyString());
        assertThat(meterRegistry
                        .counter("easyorange.ai.chat.loop", "outcome", "step_limit")
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("循环中途预算耗尽 -> 停止循环（budget），已完成的观察保留")
    void run_budgetExhaustedStopsLoop() {
        // 循环只在第 2 轮起做预算检查；首轮已执行一次工具，第 2 轮检查时余量已耗尽
        when(budgetStore.getTodayUsage("chat")).thenReturn(Optional.of(new TokenBudgetStore.TokenUsage(500_000, 0, 0)));
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn(decision("knowledge_search", "退款"), decision("knowledge_search", "退货"));
        when(retrievalService.search(anyString(), anyInt())).thenReturn(List.of());

        Result result = run("怎么退款？");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_BUDGET);
        assertThat(result.rounds()).isEqualTo(1);
        verify(aiModelSupport, times(1)).callJson(any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("决策调用故障 -> 降级按原始问题检索一次（单步降级语义），trace 不落步骤")
    void run_decisionFailureFallsBackToSingleStep() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString())).thenThrow(new RuntimeException("决策模型超时"));
        when(retrievalService.search("怎么退款？", 5))
                .thenReturn(List.of(new KnowledgeHit("kb-0002", "退款规则", "7 天无理由…", 0.9)));

        Result result = run("怎么退款？");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_DECISION_FAILED);
        assertThat(result.knowledgeHits()).hasSize(1);
        verify(retrievalService).search("怎么退款？", 5);
        verify(tracePort, never()).record(any());
        assertThat(meterRegistry
                        .counter("easyorange.ai.chat.loop", "outcome", "decision_failed")
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("决策 JSON 解析失败 -> 同样走单步降级（不把决策故障伪装成无需检索）")
    void run_unparsableDecisionFallsBackToSingleStep() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString())).thenReturn("这不是 JSON");
        when(retrievalService.search("怎么退款？", 5)).thenReturn(List.of());

        Result result = run("怎么退款？");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_DECISION_FAILED);
        verify(retrievalService).search("怎么退款？", 5);
    }

    @Test
    @DisplayName("product_detail 查无此资产 -> 有效观察不中断循环（模型可换目标）")
    void run_detailNotFoundIsAnObservation() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn(detailDecision("p-404"), decision("finish", ""));
        when(assetDetailPort.findDetail("p-404")).thenReturn(Optional.empty());

        Result result = run("看看 p-404");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        assertThat(result.details()).isEmpty();
        verify(tracePort, times(2))
                .record(argThat(trace -> trace.stepIndex() != 1
                        || (trace.success() && trace.observation().contains("未找到该资产"))));
    }

    @Test
    @DisplayName("product_detail 端口故障 -> 该步标记失败但循环继续到 finish")
    void run_detailPortFailureDoesNotKillLoop() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn(detailDecision("p-1"), decision("finish", ""));
        when(assetDetailPort.findDetail("p-1")).thenThrow(new RuntimeException("DB connection lost"));

        Result result = run("看看 p-1");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        assertThat(result.rounds()).isEqualTo(2);
        verify(tracePort, times(2))
                .record(argThat(trace -> trace.stepIndex() != 1
                        || (!trace.success() && trace.errorMsg().contains("DB connection lost"))));
    }

    @Test
    @DisplayName("未知工具名 -> 观察提示纠正，循环继续到 finish")
    void run_unknownToolGetsCorrectionObservation() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn(decision("web_browse", ""), decision("finish", ""));

        Result result = run("随便看看");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        assertThat(result.rounds()).isEqualTo(2);
        verify(tracePort)
                .record(argThat(trace -> "web_browse".equals(trace.tool())
                        && !trace.success()
                        && trace.observation().contains("未知工具")));
    }

    @Test
    @DisplayName("决策携带偏好 -> 提取并写入用户画像（长期记忆沿用）")
    void run_extractsPreference() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn("{\"thought\":\"记住偏好\",\"tool\":\"finish\",\"query\":\"\",\"productId\":null,"
                        + "\"preference\":{\"key\":\"style\",\"value\":\"复古\"}}");

        run("我喜欢复古风格的东西", "user-1", null);

        verify(preferenceRepository).record("user-1", "style", "复古");
    }

    @Test
    @DisplayName("匿名对话 -> 即便提取到偏好也不落画像")
    void run_anonymousSkipsPreference() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn("{\"thought\":\"记住偏好\",\"tool\":\"finish\",\"query\":\"\",\"productId\":null,"
                        + "\"preference\":{\"key\":\"style\",\"value\":\"复古\"}}");

        run("我喜欢复古风格的东西", "anonymous", null);

        verifyNoInteractions(preferenceRepository);
    }

    @Test
    @DisplayName("决策偏好字段缺失（模型输出空对象 {}）-> 丢弃不落库，对话照常收敛")
    void run_blankPreferenceSkipped() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn("{\"thought\":\"记住偏好\",\"tool\":\"finish\",\"query\":\"\",\"productId\":null,"
                        + "\"preference\":{}}");

        Result result = run("我想买台九成新的相机", "user-1", null);

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        verify(preferenceRepository, never()).record(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("画像落库失败 -> 只告警不抛，对话照常收敛（旁路存储不打挂主链路）")
    void run_preferenceRecordFailureNotFatal() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString()))
                .thenReturn("{\"thought\":\"记住偏好\",\"tool\":\"finish\",\"query\":\"\",\"productId\":null,"
                        + "\"preference\":{\"key\":\"style\",\"value\":\"复古\"}}");
        doThrow(new RuntimeException("db down"))
                .when(preferenceRepository)
                .record(anyString(), anyString(), anyString());

        Result result = run("我喜欢复古风格的东西", "user-1", null);

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
    }

    @Test
    @DisplayName("chatBudgetExhausted -> used + maxPerCall 超日限即 true（与入口检查同判据）")
    void chatBudgetExhausted() {
        when(budgetStore.getTodayUsage("chat")).thenReturn(Optional.of(new TokenBudgetStore.TokenUsage(299_000, 0, 0)));
        assertThat(runner.chatBudgetExhausted()).isTrue();

        when(budgetStore.getTodayUsage("chat")).thenReturn(Optional.of(new TokenBudgetStore.TokenUsage(1000, 0, 0)));
        assertThat(runner.chatBudgetExhausted()).isFalse();
    }

    @Test
    @DisplayName("非流式路径（handler 为空）-> trace 照常落库，无 step 事件")
    void run_nonStreamStillTraces() {
        when(aiModelSupport.callJson(any(), any(), anyString(), anyString())).thenReturn(decision("finish", ""));

        Result result = run("在吗？");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        verify(tracePort).record(any(AgentStepTrace.class));
    }

    private AgentLoopRunner newRunner() {
        return new AgentLoopRunner(
                aiModelSupport,
                modelRouter,
                promptRegistry,
                retrievalService,
                assetSourcingService,
                assetDetailPort,
                tracePort,
                preferenceRepository,
                budgetStore,
                aiProperties,
                new ObjectMapper(),
                idGenerator,
                meterRegistry);
    }

    /** 记录 step 事件的回调桩。 */
    private static final class RecordingHandler implements ChatStreamHandler {

        private final List<AgentStepView> steps = new ArrayList<>();

        @Override
        public void onStep(AgentStepView step) {
            steps.add(step);
        }

        @Override
        public void onToken(String token) {}

        @Override
        public void onSources(List<String> sources) {}

        @Override
        public void onDone(String fullAnswer) {}

        @Override
        public void onError(String message) {}
    }
}
