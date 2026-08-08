package com.cartethyia.easyorange.ai.adapter.inbound.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.ai.adapter.outbound.AiCallLogRecorder;
import com.cartethyia.easyorange.ai.service.AiModelSupport;
import com.cartethyia.easyorange.ai.config.AiProperties;
import java.util.List;
import java.util.Map;
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
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiEvalScheduler (LLM-as-Judge) -> 测试")
class AiEvalSchedulerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ChatModel chatModel;

    private AiProperties aiProperties;

    private AiEvalScheduler scheduler;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        scheduler = new AiEvalScheduler(jdbcTemplate, chatModel, new AiModelSupport(mock(AiCallLogRecorder.class)), aiProperties, new ObjectMapper());
    }

    private static ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    @DisplayName("开关关闭 -> 不查询不评估")
    void eval_disabled_skips() {
        aiProperties.getEval().setEnabled(false);

        scheduler.evaluateUnjudgedCalls();

        verifyNoInteractions(jdbcTemplate, chatModel);
    }

    @Test
    @DisplayName("无待评估记录 -> 直接返回")
    void eval_noCandidates() {
        aiProperties.getEval().setEnabled(true);
        when(jdbcTemplate.queryForList(anyString(), any(Integer.class))).thenReturn(List.of());

        scheduler.evaluateUnjudgedCalls();

        verify(jdbcTemplate).queryForList(anyString(), eq(50));
        verifyNoInteractions(chatModel);
    }

    @Test
    @DisplayName("Judge 打分 -> 回写 score + comment")
    void eval_judgesAndUpdates() {
        aiProperties.getEval().setEnabled(true);
        when(jdbcTemplate.queryForList(anyString(), any(Integer.class)))
                .thenReturn(List.of(Map.of("id", "log-1", "scope", "PRICING", "response_text", "{\"price\":100}")));
        when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("{\"score\": 4, \"comment\": \"定价合理准确\"}"));

        scheduler.evaluateUnjudgedCalls();

        verify(jdbcTemplate).update(anyString(), eq(4), eq("定价合理准确"), eq("log-1"));
    }

    @Test
    @DisplayName("Judge 输出非法分数 -> 跳过该条不写脏数据")
    void eval_invalidScore_skips() {
        aiProperties.getEval().setEnabled(true);
        when(jdbcTemplate.queryForList(anyString(), any(Integer.class)))
                .thenReturn(List.of(Map.of("id", "log-1", "scope", "QA", "response_text", "ok")));
        when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("{\"score\": 99, \"comment\": \"bad\"}"));

        scheduler.evaluateUnjudgedCalls();

        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Judge 输出非 JSON -> 跳过该条")
    void eval_unparseable_skips() {
        aiProperties.getEval().setEnabled(true);
        when(jdbcTemplate.queryForList(anyString(), any(Integer.class)))
                .thenReturn(List.of(Map.of("id", "log-1", "scope", "QA", "response_text", "ok")));
        when(chatModel.call(any(Prompt.class))).thenReturn(textResponse("我不知道怎么评价"));

        scheduler.evaluateUnjudgedCalls();

        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("查询失败 -> 告警返回不抛异常")
    void eval_queryFailure_swallowed() {
        aiProperties.getEval().setEnabled(true);
        when(jdbcTemplate.queryForList(anyString(), any(Integer.class)))
                .thenThrow(new RuntimeException("db down"));

        scheduler.evaluateUnjudgedCalls();

        assertThat(aiProperties.getEval().isEnabled()).isTrue();
        verifyNoInteractions(chatModel);
    }
}
