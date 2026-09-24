package com.cartethyia.easyorange.ai.application.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.chat.AgentLoopRunner.Input;
import com.cartethyia.easyorange.ai.application.chat.AgentLoopRunner.Result;
import com.cartethyia.easyorange.ai.application.retrieval.AssetSourcingService;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalService;
import com.cartethyia.easyorange.ai.application.support.AiModelRouter;
import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.model.AgentStepTrace;
import com.cartethyia.easyorange.ai.domain.model.AgentStepView;
import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.ChatSource;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.ToolCallback;
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
        runner = newRunner();
        lenient().when(modelRouter.choose("chat_tool")).thenReturn(chatModel);
        lenient().when(idGenerator.generateId()).thenReturn("trace-1");
        lenient().when(budgetStore.getTodayUsage("chat")).thenReturn(Optional.empty());
    }

    /** 原生 tool call 响应 — 模型请求调用某个工具（arguments 为 JSON 字符串）。 */
    private static ChatResponse toolCallResponse(String tool, String arguments) {
        var call = new AssistantMessage.ToolCall("call-1", "function", tool, arguments);
        var message =
                AssistantMessage.builder().content("").toolCalls(List.of(call)).build();
        return new ChatResponse(List.of(new Generation(message)));
    }

    /** 无工具调用的纯文本响应（模型没选工具 → 决策失败降级）。 */
    private static ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    /** 工具参数 JSON（模型侧契约，字段名即 {@code @ToolParam} 参数名）。 */
    private static String searchArgs(String query) {
        return "{\"thought\":\"查一下\",\"query\":\"%s\"}".formatted(query);
    }

    private static String detailArgs(String productId) {
        return "{\"thought\":\"看详情\",\"productId\":\"%s\"}".formatted(productId);
    }

    private static String finishArgs() {
        return "{\"thought\":\"闲聊无需检索\"}";
    }

    private static String rememberArgs(String key, String value) {
        return "{\"thought\":\"记住偏好\",\"preferenceKey\":\"%s\",\"preferenceValue\":\"%s\"}".formatted(key, value);
    }

    /** 连续多轮决策的桩：每轮取一个响应，超出则回空工具调用（等于模型不再选工具 → 决策失败降级）。 */
    private void stubDecisions(ChatResponse... responses) {
        var queue = new ArrayDeque<>(List.of(responses));
        when(aiModelSupport.callWithTools(any(), any(), anyString(), anyString(), anyList()))
                .thenAnswer(invocation -> toolCallsOf(queue.pollFirst()));
    }

    private static List<AssistantMessage.ToolCall> toolCallsOf(ChatResponse response) {
        if (response == null || response.getResult() == null) {
            return List.of();
        }
        return response.getResult().getOutput().getToolCalls();
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
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_FINISH, "{\"thought\":\"闲聊无需检索\",\"query\":\"\",\"productId\":null}"));
        var steps = new RecordingHandler();

        Result result = run("在吗？", "anonymous", steps);

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        assertThat(result.rounds()).isEqualTo(1);
        assertThat(result.knowledgeHits()).isEmpty();
        verifyNoInteractions(retrievalService, assetSourcingService);
        verify(tracePort)
                .record(argThat(trace -> AgentTools.TOOL_FINISH.equals(trace.tool())
                        && trace.stepIndex() == 1
                        && "trace-1".equals(trace.traceId())));
        assertThat(steps.steps).hasSize(1);
        assertThat(steps.steps.getFirst().tool()).isEqualTo(AgentTools.TOOL_FINISH);
        assertThat(steps.steps.getFirst().thought()).isEqualTo("闲聊无需检索");
        assertThat(meterRegistry
                        .counter("easyorange.ai.chat.loop", "outcome", "finished")
                        .count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.get("easyorange.ai.chat.steps").summary().count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("决策把 7 个工具的 schema（@Tool 注解生成）随请求下发 —— 供供应商侧校验与参数名锚定")
    void run_passesToolSchemasToModel() {
        stubDecisions(toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));

        run("在吗？");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ToolCallback>> callbacks = ArgumentCaptor.forClass(List.class);
        verify(aiModelSupport).callWithTools(any(), any(), anyString(), anyString(), callbacks.capture());
        Map<String, String> schemas = callbacks.getValue().stream()
                .collect(Collectors.toMap(
                        callback -> callback.getToolDefinition().name(),
                        callback -> callback.getToolDefinition().inputSchema()));

        assertThat(schemas)
                .containsOnlyKeys(
                        AgentTools.TOOL_KNOWLEDGE_SEARCH,
                        AgentTools.TOOL_PRODUCT_SEARCH,
                        AgentTools.TOOL_PRODUCT_DETAIL,
                        AgentTools.TOOL_MARKET_PRICE_STATS,
                        AgentTools.TOOL_COMPARE_ASSETS,
                        AgentTools.TOOL_REMEMBER_PREFERENCE,
                        AgentTools.TOOL_FINISH);
        // 参数名来自编译期 -parameters（缺失会退化成 arg0/arg1，模型填不对参数）
        assertThat(schemas.get(AgentTools.TOOL_KNOWLEDGE_SEARCH)).contains("thought", "query");
        assertThat(schemas.get(AgentTools.TOOL_PRODUCT_SEARCH)).contains("thought", "query");
        assertThat(schemas.get(AgentTools.TOOL_PRODUCT_DETAIL)).contains("thought", "productId");
        assertThat(schemas.get(AgentTools.TOOL_MARKET_PRICE_STATS)).contains("thought");
        assertThat(schemas.get(AgentTools.TOOL_COMPARE_ASSETS)).contains("thought", "productIds");
        assertThat(schemas.get(AgentTools.TOOL_REMEMBER_PREFERENCE))
                .contains("thought", "preferenceKey", "preferenceValue");
        // 偏好已从 finish 的参数副作用拆成独立工具，finish 不再带偏好字段（拆分的回归守卫）
        assertThat(schemas.get(AgentTools.TOOL_FINISH)).contains("thought").doesNotContain("preferenceKey");
    }

    @Test
    @DisplayName("两步循环：知识检索 -> finish -> 检索执行一次，观察进入下一轮决策上下文")
    void run_twoRoundsWithKnowledgeSearch() {
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_KNOWLEDGE_SEARCH, searchArgs("退款")),
                toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));
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
        verify(aiModelSupport, times(2)).callWithTools(any(), any(), anyString(), userMessage.capture(), anyList());
        assertThat(userMessage.getAllValues().get(1)).contains("第 1 步 [knowledge_search] 退款", "观察：命中 1 条：退款规则");
    }

    @Test
    @DisplayName("三步循环：product_search -> product_detail -> finish，观察带资产 ID、详情进 Result")
    void run_productDetailFlow() {
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_PRODUCT_SEARCH, searchArgs("5000 笔记本")),
                toolCallResponse(AgentTools.TOOL_PRODUCT_DETAIL, detailArgs("p-1")),
                toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));
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
                        AgentTools.TOOL_PRODUCT_SEARCH, AgentTools.TOOL_PRODUCT_DETAIL, AgentTools.TOOL_FINISH);

        // 召回观察带 [资产 ID]（product_detail 的 productId 取值锚点）与价格
        ArgumentCaptor<String> userMessage = ArgumentCaptor.forClass(String.class);
        verify(aiModelSupport, times(3)).callWithTools(any(), any(), anyString(), userMessage.capture(), anyList());
        assertThat(userMessage.getAllValues().get(1)).contains("[p-1] MacBook Air M1 ¥4200");
        assertThat(userMessage.getAllValues().get(2)).contains("第 2 步 [product_detail] p-1", "M1 芯片，95 新无磕碰");

        // 详情步骤的 trace 带入参 productId
        verify(tracePort, times(3))
                .record(argThat(trace ->
                        !AgentTools.TOOL_PRODUCT_DETAIL.equals(trace.tool()) || "p-1".equals(trace.toolInput())));
    }

    @Test
    @DisplayName("compare_assets：一次比对多件候选（替代逐件 product_detail），确定性结论进下一轮决策")
    void run_compareAssetsFlow() {
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_PRODUCT_SEARCH, searchArgs("5000 笔记本")),
                toolCallResponse(AgentTools.TOOL_COMPARE_ASSETS, compareArgs(List.of("p-1", "p-2"))),
                toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));
        when(assetSourcingService.search("5000 笔记本", 5))
                .thenReturn(List.of(
                        new AssetHit("p-1", "MacBook Air M1", BigDecimal.valueOf(4200), "数码", "轻微使用痕迹", 0.83),
                        new AssetHit("p-2", "ThinkPad X1", BigDecimal.valueOf(4800), "数码", "几乎全新", 0.79)));
        // 便宜的那件成色差一档、贵的那件成色好——两维各自给出胜出方（不是同一件包揽）
        when(assetDetailPort.findDetail("p-1"))
                .thenReturn(Optional.of(detail("p-1", BigDecimal.valueOf(4200), "轻微使用痕迹")));
        when(assetDetailPort.findDetail("p-2"))
                .thenReturn(Optional.of(detail("p-2", BigDecimal.valueOf(4800), "几乎全新")));
        var steps = new RecordingHandler();

        Result result = run("预算 5000 想买笔记本，帮我挑一台", "user-1", steps);

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        // 一次 compare_assets 拿到两件详情（等价于两次 product_detail）但只花一步
        assertThat(result.details()).hasSize(2);
        assertThat(steps.steps)
                .extracting(AgentStepView::tool)
                .containsExactly(
                        AgentTools.TOOL_PRODUCT_SEARCH, AgentTools.TOOL_COMPARE_ASSETS, AgentTools.TOOL_FINISH);

        // 比对结论是代码算的，进下一轮决策上下文（模型据此取舍，不用自己心算）
        ArgumentCaptor<String> userMessage = ArgumentCaptor.forClass(String.class);
        verify(aiModelSupport, times(3)).callWithTools(any(), any(), anyString(), userMessage.capture(), anyList());
        assertThat(userMessage.getAllValues().get(2)).contains("价格：p-1 最低 ¥4200", "成色：p-2 成色最好（几乎全新）");

        // compare_assets 的 trace 带入参 ID 列表
        verify(tracePort, times(3))
                .record(argThat(trace ->
                        !AgentTools.TOOL_COMPARE_ASSETS.equals(trace.tool()) || "p-1、p-2".equals(trace.toolInput())));
    }

    @Test
    @DisplayName("compare_assets 少于 2 个 ID -> 回观察文本而非失败，模型可换 ID 重试")
    void run_compareAssetsTooFewIds() {
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_COMPARE_ASSETS, compareArgs(List.of("p-1"))),
                toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));

        Result result = run("比一下", "user-1", null);

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        verifyNoInteractions(assetDetailPort);
    }

    @Test
    @DisplayName("market_price_stats：对已召回资产出行情统计（零模型计算），口径与 MarketAnalysisTool 一致")
    void run_marketPriceStatsFlow() {
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_PRODUCT_SEARCH, searchArgs("5000 笔记本")),
                toolCallResponse(AgentTools.TOOL_MARKET_PRICE_STATS, "{\"thought\":\"看行情\"}"),
                toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));
        when(assetSourcingService.search("5000 笔记本", 5))
                .thenReturn(List.of(
                        new AssetHit("p-1", "MacBook Air M1", BigDecimal.valueOf(4200), "数码", "九五新", 0.83),
                        new AssetHit("p-2", "ThinkPad X1", BigDecimal.valueOf(4800), "数码", "九五新", 0.79)));
        var steps = new RecordingHandler();

        Result result = run("预算 5000 想买笔记本", "user-1", steps);

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        assertThat(steps.steps.get(1).tool()).isEqualTo(AgentTools.TOOL_MARKET_PRICE_STATS);
        assertThat(steps.steps.get(1).observation()).isEqualTo("当前 2 件在售，均价 ¥4500，价格区间 ¥4200-¥4800");
        // 行情统计只吃已召回资产，不额外读详情
        verifyNoInteractions(assetDetailPort);
    }

    /** compare_assets 的工具参数 JSON（productIds 是数组，与 {@code @ToolParam} 的 List 参数对齐）。 */
    private static String compareArgs(List<String> productIds) {
        return "{\"thought\":\"比一比\",\"productIds\":[%s]}"
                .formatted(productIds.stream().map(id -> "\"%s\"".formatted(id)).collect(Collectors.joining(",")));
    }

    private static AssetDetail detail(String id, BigDecimal price, String conditionDesc) {
        return new AssetDetail(id, "资产 " + id, "描述", price, "数码", conditionDesc, "上海", "liming", "ONLINE");
    }

    @Test
    @DisplayName("步数上限内未 finish -> 强制收敛（step_limit），不再发第 N+1 次决策")
    void run_stepLimitForcesConvergence() {
        aiProperties = PropertyBindings.bind(AiProperties.class, "chat.max-steps", "2");
        runner = newRunner();
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_KNOWLEDGE_SEARCH, searchArgs("退款")),
                toolCallResponse(AgentTools.TOOL_KNOWLEDGE_SEARCH, searchArgs("退货")),
                toolCallResponse(AgentTools.TOOL_KNOWLEDGE_SEARCH, searchArgs("换货")));
        when(retrievalService.search(anyString(), anyInt())).thenReturn(List.of());

        Result result = run("反复问规则");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_STEP_LIMIT);
        assertThat(result.rounds()).isEqualTo(2);
        verify(aiModelSupport, times(2)).callWithTools(any(), any(), anyString(), anyString(), anyList());
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
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_KNOWLEDGE_SEARCH, searchArgs("退款")),
                toolCallResponse(AgentTools.TOOL_KNOWLEDGE_SEARCH, searchArgs("退货")));
        when(retrievalService.search(anyString(), anyInt())).thenReturn(List.of());

        Result result = run("怎么退款？");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_BUDGET);
        assertThat(result.rounds()).isEqualTo(1);
        verify(aiModelSupport, times(1)).callWithTools(any(), any(), anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("决策调用故障 -> 降级按原始问题检索一次（单步降级语义），trace 不落步骤")
    void run_decisionFailureFallsBackToSingleStep() {
        when(aiModelSupport.callWithTools(any(), any(), anyString(), anyString(), anyList()))
                .thenThrow(new RuntimeException("决策模型超时"));
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
    @DisplayName("模型未返回工具调用（直接回文本）-> 同样走单步降级（决策失败语义的原生形态）")
    void run_noToolCallFallsBackToSingleStep() {
        stubDecisions(textResponse("我认为不需要检索"));
        when(retrievalService.search("怎么退款？", 5)).thenReturn(List.of());

        Result result = run("怎么退款？");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_DECISION_FAILED);
        verify(retrievalService).search("怎么退款？", 5);
    }

    @Test
    @DisplayName("工具调用参数 JSON 不可解析 -> 同样走单步降级（不把决策故障伪装成无需检索）")
    void run_unparsableToolArgumentsFallBackToSingleStep() {
        stubDecisions(toolCallResponse(AgentTools.TOOL_KNOWLEDGE_SEARCH, "这不是 JSON"));
        when(retrievalService.search("怎么退款？", 5)).thenReturn(List.of());

        Result result = run("怎么退款？");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_DECISION_FAILED);
        verify(retrievalService).search("怎么退款？", 5);
    }

    @Test
    @DisplayName("供应商侧并行工具调用 -> 每步只执行第一个，其余不进观察（循环按每步一个工具推进）")
    void run_parallelToolCallsExecuteFirstOnly() {
        var first =
                new AssistantMessage.ToolCall("call-1", "function", AgentTools.TOOL_KNOWLEDGE_SEARCH, searchArgs("退款"));
        var second =
                new AssistantMessage.ToolCall("call-2", "function", AgentTools.TOOL_PRODUCT_SEARCH, searchArgs("笔记本"));
        var parallel = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(first, second))
                .build();
        stubDecisions(
                new ChatResponse(List.of(new Generation(parallel))),
                toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));
        when(retrievalService.search("退款", 5)).thenReturn(List.of());

        Result result = run("怎么退款？");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        verify(retrievalService).search("退款", 5);
        verifyNoInteractions(assetSourcingService);
    }

    @Test
    @DisplayName("product_detail 查无此资产 -> 有效观察不中断循环（模型可换目标）")
    void run_detailNotFoundIsAnObservation() {
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_PRODUCT_DETAIL, detailArgs("p-404")),
                toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));
        when(assetDetailPort.findDetail("p-404")).thenReturn(Optional.empty());

        Result result = run("看看 p-404");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        assertThat(result.details()).isEmpty();
        verify(tracePort, times(2))
                .record(argThat(trace -> trace.stepIndex() != 1
                        || (trace.success() && trace.observation().contains("未找到该资产"))));
    }

    @Test
    @DisplayName("product_detail 端口故障 -> 该步标记失败但循环继续到 finish（工具失败不打断对话）")
    void run_detailPortFailureDoesNotKillLoop() {
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_PRODUCT_DETAIL, detailArgs("p-1")),
                toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));
        when(assetDetailPort.findDetail("p-1")).thenThrow(new RuntimeException("DB connection lost"));

        Result result = run("看看 p-1");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        assertThat(result.rounds()).isEqualTo(2);
        verify(tracePort, times(2))
                .record(argThat(trace -> trace.stepIndex() != 1
                        || (!trace.success() && trace.errorMsg().contains("DB connection lost"))));
    }

    @Test
    @DisplayName("检索工具内部故障 -> 收敛成失败观察交回模型（带错误反馈的修复轮），循环不断")
    void run_toolFailureBecomesObservation() {
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_KNOWLEDGE_SEARCH, searchArgs("退款")),
                toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));
        when(retrievalService.search("退款", 5)).thenThrow(new RuntimeException("embedding provider down"));

        Result result = run("怎么退款？");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        assertThat(result.rounds()).isEqualTo(2);
        verify(tracePort, times(2)).record(any(AgentStepTrace.class));
        verify(tracePort)
                .record(argThat(trace -> trace.stepIndex() == 1
                        && !trace.success()
                        && trace.errorMsg().contains("embedding provider down")));
    }

    @Test
    @DisplayName("未知工具名 -> 观察提示纠正，循环继续到 finish")
    void run_unknownToolGetsCorrectionObservation() {
        stubDecisions(toolCallResponse("web_browse", "{}"), toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));

        Result result = run("随便看看");

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        assertThat(result.rounds()).isEqualTo(2);
        verify(tracePort)
                .record(argThat(trace -> "web_browse".equals(trace.tool())
                        && !trace.success()
                        && trace.observation().contains("未知工具")));
    }

    @Test
    @DisplayName("remember_preference -> 写入用户画像（长期记忆，模型自主决定的一步）")
    void run_extractsPreference() {
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_REMEMBER_PREFERENCE, rememberArgs("style", "复古")),
                toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));

        run("我喜欢复古风格的东西", "user-1", null);

        verify(preferenceRepository).record("user-1", "style", "复古");
    }

    @Test
    @DisplayName("remember_preference 的观察与其余执行类工具同一种：纯文本，不带默认转换器的 JSON 引号")
    void run_preferenceObservationIsPlainText() {
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_REMEMBER_PREFERENCE, rememberArgs("style", "复古")),
                toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));

        run("我喜欢复古风格的东西", "user-1", null);

        verify(tracePort)
                .record(argThat(trace -> AgentTools.TOOL_REMEMBER_PREFERENCE.equals(trace.tool())
                        && "已记录偏好：style = 复古".equals(trace.observation())));
    }

    @Test
    @DisplayName("匿名对话 -> 即便模型调了 remember_preference 也不落画像")
    void run_anonymousSkipsPreference() {
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_REMEMBER_PREFERENCE, rememberArgs("style", "复古")),
                toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));

        run("我喜欢复古风格的东西", "anonymous", null);

        verifyNoInteractions(preferenceRepository);
    }

    @Test
    @DisplayName("remember_preference 收到空值 -> 跳过落库并回观察文本，对话照常收敛")
    void run_blankPreferenceSkipped() {
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_REMEMBER_PREFERENCE, rememberArgs("", "")),
                toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));

        Result result = run("我想买台九成新的相机", "user-1", null);

        assertThat(result.outcome()).isEqualTo(AgentLoopRunner.OUTCOME_FINISHED);
        verify(preferenceRepository, never()).record(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("画像落库失败 -> 收敛成失败观察交回模型，对话照常收敛（旁路存储不打挂主链路）")
    void run_preferenceRecordFailureNotFatal() {
        stubDecisions(
                toolCallResponse(AgentTools.TOOL_REMEMBER_PREFERENCE, rememberArgs("style", "复古")),
                toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));
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
        stubDecisions(toolCallResponse(AgentTools.TOOL_FINISH, finishArgs()));

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
        public void onSources(List<ChatSource> sources) {}

        @Override
        public void onDone(String fullAnswer) {}

        @Override
        public void onError(String message) {}
    }
}
