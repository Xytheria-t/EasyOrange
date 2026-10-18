package com.cartethyia.easyorange.ai.application.toolcall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.chat.ChatTools;
import com.cartethyia.easyorange.ai.application.support.ModelCaller;
import com.cartethyia.easyorange.ai.domain.enums.ModelCallScope;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import tools.jackson.databind.ObjectMapper;

@DisplayName("ToolCallDecider 测试 — 决策故障的重试阶梯")
class ToolCallDeciderTest {

    private static final ToolCallback UNUSED_CALLBACK = mock(ToolCallback.class);

    private ToolCallDecider decider(ModelCaller modelCaller) {
        return new ToolCallDecider(modelCaller, new ObjectMapper());
    }

    private List<ToolLoopCall> loopCalls(ModelCaller modelCaller) {
        return decider(modelCaller)
                .decide(
                        "sess-1",
                        List.of(new UserMessage("退款规则")),
                        List.of(UNUSED_CALLBACK),
                        ModelCallScope.CHAT,
                        d -> null);
    }

    @Test
    @DisplayName("首次决策故障重试一次后成功 — 抖动自愈，不落入单步降级")
    void retry_recoversFromTransientFailure() {
        ModelCaller modelCaller = mock(ModelCaller.class);
        when(modelCaller.callWithTools(any(), any(), anyList(), anyList()))
                .thenThrow(new RuntimeException("connection reset"))
                .thenReturn(List.of(toolCall("{\"thought\":\"查规则\",\"query\":\"退款\"}")));

        List<ToolLoopCall> calls = loopCalls(modelCaller);

        assertThat(calls).hasSize(1);
        assertThat(calls.get(0).rawArguments()).contains("退款");
        verify(modelCaller, times(2)).callWithTools(any(), any(), anyList(), anyList());
    }

    @Test
    @DisplayName("重试仍失败才交编排器降级 — 返回空列表且调用恰好两次")
    void retry_givesUpAfterSecondFailure() {
        ModelCaller modelCaller = mock(ModelCaller.class);
        when(modelCaller.callWithTools(any(), any(), anyList(), anyList()))
                .thenThrow(new RuntimeException("timeout"))
                .thenThrow(new RuntimeException("timeout"));

        List<ToolLoopCall> calls = loopCalls(modelCaller);

        assertThat(calls).isEmpty();
        verify(modelCaller, times(2)).callWithTools(any(), any(), anyList(), anyList());
    }

    @Test
    @DisplayName("模型未返回工具调用不重试 — 协议层确定性响应，重试同一输入只会白付一次调用")
    void noRetry_whenModelReturnsNoToolCalls() {
        ModelCaller modelCaller = mock(ModelCaller.class);
        when(modelCaller.callWithTools(any(), any(), anyList(), anyList())).thenReturn(List.of());

        List<ToolLoopCall> calls = loopCalls(modelCaller);

        assertThat(calls).isEmpty();
        verify(modelCaller, times(1)).callWithTools(any(), any(), anyList(), anyList());
    }

    private AssistantMessage.ToolCall toolCall(String arguments) {
        return new AssistantMessage.ToolCall("call-1", "function", ChatTools.TOOL_KNOWLEDGE_SEARCH, arguments);
    }
}
