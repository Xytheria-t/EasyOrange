package com.cartethyia.easyorange.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.adapter.outbound.AiCallLogRecorder;
import java.util.List;
import java.util.Optional;
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
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiJudge (LLM-as-Judge) -> 测试")
class AiJudgeTest {

    @Mock
    private ChatModel chatModel;

    private AiJudge aiJudge;

    @BeforeEach
    void setUp() {
        aiJudge = new AiJudge(chatModel, new AiModelSupport(mock(AiCallLogRecorder.class)), new ObjectMapper());
    }

    private static ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    @DisplayName("通用评审 -> 返回分数与评语")
    void judge_scores() {
        when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("{\"score\": 4, \"comment\": \"准确\"}"));

        Optional<AiJudge.Judgement> result = aiJudge.judge("CHAT", "回答内容");

        assertThat(result).isPresent();
        assertThat(result.get().score()).isEqualTo(4);
        assertThat(result.get().comment()).isEqualTo("准确");
    }

    @Test
    @DisplayName("对照参考评审 -> 语义一致判定")
    void judge_againstReference() {
        when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("{\"score\": 5, \"comment\": \"一致\"}"));

        Optional<AiJudge.Judgement> result = aiJudge.judgeAgainstReference("参考", "AI 回答");

        assertThat(result).isPresent();
        assertThat(result.get().score()).isEqualTo(5);
    }

    @Test
    @DisplayName("分数越界 -> empty（不写脏数据）")
    void judge_invalidScore_empty() {
        when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("{\"score\": 99, \"comment\": \"bad\"}"));

        assertThat(aiJudge.judge("CHAT", "x")).isEmpty();
    }

    @Test
    @DisplayName("非 JSON 输出 -> empty")
    void judge_unparseable_empty() {
        when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("我不知道"));

        assertThat(aiJudge.judge("CHAT", "x")).isEmpty();
    }

    @Test
    @DisplayName("模型异常 -> empty（调用方跳过该条）")
    void judge_modelError_empty() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("down"));

        assertThat(aiJudge.judge("CHAT", "x")).isEmpty();
    }
}
