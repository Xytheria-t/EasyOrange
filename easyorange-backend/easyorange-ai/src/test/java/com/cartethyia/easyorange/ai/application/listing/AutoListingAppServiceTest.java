package com.cartethyia.easyorange.ai.application.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.application.retrieval.AssetSourcingAppService;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalAppService;
import com.cartethyia.easyorange.ai.application.support.AiModelRouter;
import com.cartethyia.easyorange.ai.application.support.ToolCallDecider;
import com.cartethyia.easyorange.ai.application.support.ToolCallLoopOutcome;
import com.cartethyia.easyorange.ai.application.support.ToolLoopKernel;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.model.ChatSource;
import com.cartethyia.easyorange.ai.domain.model.ToolCallStepView;
import com.cartethyia.easyorange.ai.domain.port.CategoryCatalogPort;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistryPort;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort;
import com.cartethyia.easyorange.ai.domain.port.ToolCallStepTracePort;
import com.cartethyia.easyorange.ai.testsupport.PropertyBindings;
import com.cartethyia.easyorange.ai.testsupport.TestAiModelSupport;
import com.cartethyia.easyorange.ai.testsupport.TestPromptRegistry;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import tools.jackson.databind.ObjectMapper;

/**
 * 发布助手全链路测试 — 视觉预识别 → 工具循环 → 生成，模型全部走桩（真内核 + 真 AiModelSupport +
 * 桩 ChatModel），覆盖步骤事件、降级口径与取图失败语义。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AutoListingAppService 测试（多步链路）")
class AutoListingAppServiceTest {

    private static final String CLUE_JSON = """
            {"itemName":"索尼 A7M3 微单","categoryGuess":"相机","conditionHint":"外观九成新","visibleDetails":["单机身","原厂背带"]}
            """;

    private static final String VALID_LISTING_JSON = """
            {"title":"索尼 A7M3 微单","description":"九成新单机身","price":8500,
            "categoryName":"手机数码","conditionLevel":"2","location":"上海"}
            """;

    @Mock
    private AiModelRouter modelRouter;

    @Mock
    private CategoryCatalogPort categoryCatalogPort;

    @Mock
    private VisionImageLoader visionImageLoader;

    private final ChatModel visionChatModel = mock(ChatModel.class);
    private final ChatModel decisionChatModel = mock(ChatModel.class);
    private final ArrayDeque<String> visionResponses = new ArrayDeque<>();
    private final ArrayDeque<ChatResponse> decisionResponses = new ArrayDeque<>();

    private AutoListingAppService service;

    @BeforeEach
    void setUp() {
        lenient().when(modelRouter.choose("vision")).thenReturn(visionChatModel);
        lenient().when(modelRouter.choose("chat_tool")).thenReturn(decisionChatModel);
        lenient().when(categoryCatalogPort.listAvailableCategoryNames()).thenReturn(List.of("手机数码", "图书教材"));
        lenient()
                .when(visionChatModel.call(any(Prompt.class)))
                .thenAnswer(inv -> textResponse(visionResponses.pollFirst()));
        lenient().when(decisionChatModel.call(any(Prompt.class))).thenAnswer(inv -> {
            ChatResponse next = decisionResponses.pollFirst();
            return next != null ? next : textResponse("(无工具调用)");
        });
        // 取图转 data URL 是 I/O 边界，单测直通透传（取图行为由 VisionImageLoaderTest 覆盖）
        lenient().when(visionImageLoader.toDataUrls(any())).thenAnswer(inv -> inv.getArgument(0));

        service = newService(new TestPromptRegistry());
    }

    private AutoListingAppService newService(PromptRegistryPort promptRegistry) {
        return new AutoListingAppService(
                modelRouter,
                promptRegistry,
                TestAiModelSupport.create(),
                categoryCatalogPort,
                visionImageLoader,
                new ToolLoopKernel(mock(ToolCallStepTracePort.class), stubIdGenerator()),
                new ListingToolsFactory(
                        mock(KnowledgeRetrievalAppService.class),
                        mock(AssetSourcingAppService.class),
                        categoryCatalogPort),
                new ToolCallDecider(TestAiModelSupport.create(), modelRouter, new ObjectMapper()),
                mock(TokenBudgetStorePort.class),
                PropertyBindings.bind(AiProperties.class),
                new ListingLoopMetrics(new SimpleMeterRegistry()));
    }

    private static IdGenerator stubIdGenerator() {
        IdGenerator generator = mock(IdGenerator.class);
        lenient().when(generator.generateId()).thenReturn("trace-1");
        return generator;
    }

    private static ChatResponse textResponse(@Nullable String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    /** 决策桩 — 依次返回模型要调的工具（入参格式「工具名:arguments JSON」），耗尽后回纯文本（决策失败降级）。 */
    private void stubDecisions(String... toolCalls) {
        for (String call : toolCalls) {
            String[] parts = call.split(":", 2);
            decisionResponses.add(new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                    .content("")
                    .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", parts[0], parts[1])))
                    .build()))));
        }
    }

    private void enqueueVision(String... jsons) {
        visionResponses.addAll(List.of(jsons));
    }

    @Nested
    @DisplayName("analyzeImages 全链路")
    class FullFlowTests {

        @Test
        @DisplayName("预识别 → 决策 → 生成：字段落表，视觉模型至少两次（线索 + 生成）")
        void fullFlow() {
            enqueueVision(CLUE_JSON, VALID_LISTING_JSON);
            stubDecisions("finish:{\"thought\":\"信息已足够\"}");
            var steps = new RecordingHandler();

            AutoListingResult result = service.analyzeImages(List.of("http://example.com/a.jpg"), "user-1", steps);

            assertThat(result.title()).isEqualTo("索尼 A7M3 微单");
            assertThat(result.price()).isEqualByComparingTo(new BigDecimal("8500"));
            assertThat(result.categoryName()).isEqualTo("手机数码");
            assertThat(steps.tools).containsExactly("finish");
            verify(visionChatModel, atLeast(2)).call(any(Prompt.class));
        }

        @Test
        @DisplayName("线索识别失败（模型返回不可解析）-> 抛 B8002，不进循环也不生成")
        void clueFailure_failsFast() {
            enqueueVision("not json");

            assertThatThrownBy(() -> service.analyzeImages(List.of("http://example.com/a.jpg")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo("B8002");
            verify(decisionChatModel, never()).call(any(Prompt.class));
        }

        @Test
        @DisplayName("取图失败 -> 降级为 B8002，不把坏图发给供应商")
        void imageLoadFailure_failsFast() {
            when(visionImageLoader.toDataUrls(any())).thenThrow(new IllegalStateException("图片下载失败: HTTP 404"));

            assertThatThrownBy(() -> service.analyzeImages(List.of("http://example.com/gone.jpg")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo("B8002");
        }

        @Test
        @DisplayName("生成失败（空输出）-> 抛 B8002")
        void generateFailure_failsExplicitly() {
            enqueueVision(CLUE_JSON, "");
            stubDecisions("finish:{\"thought\":\"直接收敛\"}");

            assertThatThrownBy(() -> service.analyzeImages(List.of("http://example.com/a.jpg")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo("B8002");
        }

        @Test
        @DisplayName("决策失败（模型不回工具调用）-> 循环降级不报错，空观察照常进生成")
        void decisionFailed_degradesToGeneration() {
            enqueueVision(CLUE_JSON, VALID_LISTING_JSON);
            decisionResponses.add(textResponse("我觉得不用查"));

            AutoListingResult result = service.analyzeImages(List.of("http://example.com/a.jpg"));

            // 降级链路照常产出表单：行情缺失由生成器按 prompt 约束置 price = null，这里验证链路不死
            assertThat(result.title()).isEqualTo("索尼 A7M3 微单");
            assertThat(result.categoryName()).isEqualTo("手机数码");
        }

        @Test
        @DisplayName("Prompt 模板缺失时抛 IllegalStateException（配置错误 fail-fast，不伪装成 AI 不可用）")
        void missingPrompt_failsFast() {
            enqueueVision(CLUE_JSON);
            service = newService(TestPromptRegistry.empty());

            assertThatThrownBy(() -> service.analyzeImages(List.of("http://example.com/a.jpg")))
                    .isInstanceOf(IllegalStateException.class);
            verify(visionChatModel, never()).call(any(Prompt.class));
        }
    }

    @Nested
    @DisplayName("runToolLoop（评估入口，只跑循环）")
    class LoopOnlyTests {

        @Test
        @DisplayName("卖家线索文本进首轮上下文：禁售 → finish 两步收敛，toolPath 完整")
        void loopWithoutImages_usesSellerNote() {
            stubDecisions(
                    "knowledge_search:{\"thought\":\"查禁售\",\"query\":\"相机 禁售\"}", "finish:{\"thought\":\"禁售不再定价\"}");

            ListingLoopResult result = service.runToolLoop(null, "佳能单反相机 九成新 想出掉", "eval-listing-001", null, null);

            assertThat(result.outcome()).isEqualTo(ToolCallLoopOutcome.FINISHED);
            assertThat(result.toolPath()).containsExactly("knowledge_search", "finish");
            assertThat(result.rounds()).isEqualTo(2);
        }

        @Test
        @DisplayName("机器归属（评估跑批传 null userId）-> 循环照常跑，toolPath 只含真实工具")
        void loopRunsForMachineSubject() {
            stubDecisions("finish:{\"thought\":\"线索已足够\"}");

            ListingLoopResult result = service.runToolLoop(null, "拍了几张耳机", null, null, null);

            assertThat(result.outcome()).isEqualTo(ToolCallLoopOutcome.FINISHED);
            assertThat(result.toolPath()).containsExactly("finish");
        }
    }

    /** 记录 step 事件的回调桩。 */
    private static final class RecordingHandler implements ChatStreamHandler {

        private final List<String> tools = new ArrayList<>();

        @Override
        public void onStep(ToolCallStepView step) {
            tools.add(step.tool());
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
