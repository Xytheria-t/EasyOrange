package com.cartethyia.easyorange.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.adapter.outbound.AiCallLogRecorder;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiModelSupport 流式调用 -> 测试")
class AiModelSupportStreamTest {

    @Mock
    private ChatModel chatModel;

    private AiModelSupport aiModelSupport;

    @BeforeEach
    void setUp() {
        aiModelSupport = new AiModelSupport(mock(AiCallLogRecorder.class));
    }

    private static ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    @DisplayName("流式调用逐 token 回调并返回完整拼接文本")
    void stream_collectsTokens() {
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(textResponse("你"), textResponse("好")));

        var collected = new StringBuilder();
        String full = aiModelSupport.callTextStream(chatModel, AiCallScope.CHAT, "system", "user", collected::append);

        assertThat(full).isEqualTo("你好");
        assertThat(collected.toString()).isEqualTo("你好");
    }

    @Test
    @DisplayName("空 token 不回调（模型输出空段）")
    void stream_skipsEmptyTokens() {
        when(chatModel.stream(any(Prompt.class)))
                .thenReturn(Flux.just(textResponse(""), textResponse("答"), textResponse("")));

        var collected = new StringBuilder();
        String full = aiModelSupport.callTextStream(chatModel, AiCallScope.CHAT, "system", "user", collected::append);

        assertThat(full).isEqualTo("答");
        assertThat(collected.toString()).isEqualTo("答");
    }

    @Test
    @DisplayName("流式异常 -> 异常抛出且不落成功日志")
    void stream_errorPropagates() {
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.error(new RuntimeException("model down")));

        var holder = new AtomicReference<Throwable>();
        try {
            aiModelSupport.callTextStream(chatModel, AiCallScope.CHAT, "system", "user", t -> {});
        } catch (Exception e) {
            holder.set(e);
        }

        assertThat(holder.get()).isInstanceOf(RuntimeException.class).hasMessageContaining("model down");
        verify(chatModel, never()).call(any(Prompt.class));
    }
}
