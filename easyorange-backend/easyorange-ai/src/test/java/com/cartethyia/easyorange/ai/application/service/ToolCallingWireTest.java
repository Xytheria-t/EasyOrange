package com.cartethyia.easyorange.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.adapter.outbound.budget.InMemoryTokenBudgetStore;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import com.cartethyia.easyorange.ai.domain.model.AgentStepDecision;
import com.cartethyia.easyorange.ai.domain.port.AiCallLogPort;
import com.cartethyia.easyorange.ai.testsupport.PropertyBindings;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.setup.OpenAiSetup;
import org.springframework.ai.support.ToolCallbacks;
import tools.jackson.databind.ObjectMapper;

/**
 * 原生 tool calling 的 wire 契约测试 — 用 JDK 自带 {@link HttpServer} 当供应商桩，校验经
 * {@link AiModelSupport#callWithTools} 发出的**真实请求体**与回来的 tool call 解析。
 * <p>
 * 覆盖三件会「静默失效」的事（都不是单元测试能看出来的）：
 * <ol>
 *   <li>工具定义确实进了请求体 {@code tools}（丢了 = 模型永不调工具，整条对话降级成单步检索）；</li>
 *   <li>per-request options 继承了模型名（缺 {@code model} 时 openai-java 回退 SDK 默认模型名，
 *       对非 OpenAI 供应商直接 404）；</li>
 *   <li>工具轮不带 {@code response_format}（JSON 模式与 tools 同发在部分供应商会冲突），
 *       且响应里的 tool call 经 Spring AI 的 OpenAI 映射层能被正确解析出来。</li>
 * </ol>
 */
@DisplayName("原生 tool calling wire 契约（HTTP 桩）-> 测试")
class ToolCallingWireTest {

    private static final String TOOL_CALL_RESPONSE = """
            {
              "id": "chatcmpl-stub",
              "object": "chat.completion",
              "created": 1758300000,
              "model": "deepseek-chat",
              "choices": [{
                "index": 0,
                "message": {
                  "role": "assistant",
                  "content": "",
                  "tool_calls": [{
                    "id": "call-1",
                    "type": "function",
                    "function": {"name": "knowledge_search", "arguments": "{\\"thought\\":\\"查退款规则\\",\\"query\\":\\"退款\\"}"}
                  }]
                },
                "finish_reason": "tool_calls"
              }],
              "usage": {"prompt_tokens": 120, "completion_tokens": 18, "total_tokens": 138}
            }
            """;

    private final AtomicReference<String> capturedBody = new AtomicReference<>();
    private HttpServer server;

    @BeforeEach
    void startStubProvider() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = TOOL_CALL_RESPONSE.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopStubProvider() {
        server.stop(0);
    }

    @Test
    @DisplayName("请求体带 5 个工具的 schema 与模型名、不带 response_format；响应的 tool call 可解析")
    void callWithTools_wireContract() throws Exception {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        ChatModel chatModel = stubChatModel(baseUrl);
        var support = new AiModelSupport(
                noopCallLog(),
                new InMemoryTokenBudgetStore(),
                PropertyBindings.bind(AiProperties.class),
                new ObjectMapper());
        var tools = new AgentTools(List.of(), List.of(), List.of(), null, null, null, null, null);

        List<AssistantMessage.ToolCall> calls = support.callWithTools(
                chatModel, AiCallScope.CHAT, "你是多步工具决策器", "用户问题：怎么退款？", List.of(ToolCallbacks.from(tools)));

        String request = capturedBody.get();
        assertThat(request).contains("\"tools\"");
        assertThat(request)
                .contains(
                        AgentTools.TOOL_KNOWLEDGE_SEARCH,
                        AgentTools.TOOL_PRODUCT_SEARCH,
                        AgentTools.TOOL_PRODUCT_DETAIL,
                        AgentTools.TOOL_REMEMBER_PREFERENCE,
                        AgentTools.TOOL_FINISH);
        assertThat(request).contains("\"thought\"", "\"query\"", "\"productId\"", "\"preferenceKey\"");
        assertThat(request).contains("\"model\":\"deepseek-chat\"");
        assertThat(request).doesNotContain("response_format");

        // 回来的 tool call 经 Spring AI OpenAI 映射层解析后，参数字段与 {@code @ToolParam} 名对齐
        assertThat(calls).hasSize(1);
        assertThat(calls.getFirst().name()).isEqualTo(AgentTools.TOOL_KNOWLEDGE_SEARCH);
        AgentStepDecision parsed = new ObjectMapper().readValue(calls.getFirst().arguments(), AgentStepDecision.class);
        assertThat(parsed.thought()).isEqualTo("查退款规则");
        assertThat(parsed.query()).isEqualTo("退款");
    }

    private static ChatModel stubChatModel(String baseUrl) {
        var observations = ObservationRegistry.NOOP;
        var client = OpenAiSetup.setupSyncClient(
                baseUrl,
                "test-key",
                null,
                null,
                null,
                null,
                false,
                false,
                "deepseek-chat",
                Duration.ofSeconds(5),
                0,
                null,
                java.util.Map.of(),
                observations,
                new SimpleMeterRegistry(),
                List.of());
        return OpenAiChatModel.builder()
                .openAiClient(client)
                .options(OpenAiChatOptions.builder()
                        .baseUrl(baseUrl)
                        .apiKey("test-key")
                        .model("deepseek-chat")
                        .build())
                .observationRegistry(observations)
                .build();
    }

    private static AiCallLogPort noopCallLog() {
        return (scope,
                model,
                promptHash,
                response,
                latencyMs,
                tokenInput,
                tokenOutput,
                subjectId,
                success,
                errorMsg) -> {};
    }
}
