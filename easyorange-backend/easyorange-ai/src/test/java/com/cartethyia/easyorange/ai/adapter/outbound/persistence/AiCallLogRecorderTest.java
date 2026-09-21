package com.cartethyia.easyorange.ai.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.idgen.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiCallLogRecorder -> 测试")
class AiCallLogRecorderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private IdGenerator idGenerator;

    private AiCallLogRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new AiCallLogRecorder(jdbcTemplate, idGenerator);
        when(idGenerator.generateId()).thenReturn("id-1");
    }

    @Test
    @DisplayName("成功调用 -> insert 完整字段（含用量）")
    void record_success() {
        recorder.record("PRICING", "OpenAiChatModel", "abc123", "{\"price\":100}", 250L, 120, 40, true, null);

        verify(jdbcTemplate)
                .update(
                        anyString(),
                        eq("id-1"),
                        eq("PRICING"),
                        eq("OpenAiChatModel"),
                        eq("abc123"),
                        eq("{\"price\":100}"),
                        eq(250L),
                        eq(120),
                        eq(40),
                        eq(1),
                        eq(null));
    }

    @Test
    @DisplayName("未回报用量 -> 记 0（不估算，估算值混进成本报表比缺数据更危险）")
    void record_withoutUsage_recordsZero() {
        recorder.record("SEMANTIC", "OpenAiEmbeddingModel", "hash", null, 30L, 0, 0, true, null);

        verify(jdbcTemplate)
                .update(
                        anyString(),
                        eq("id-1"),
                        eq("SEMANTIC"),
                        eq("OpenAiEmbeddingModel"),
                        eq("hash"),
                        eq(null),
                        eq(30L),
                        eq(0),
                        eq(0),
                        eq(1),
                        eq(null));
    }

    @Test
    @DisplayName("负数用量 -> 归零（供应商回报异常不该污染成本报表）")
    void record_negativeUsageClampedToZero() {
        recorder.record("QA", "model", "hash", "ok", 1L, -5, -1, true, null);

        verify(jdbcTemplate)
                .update(
                        anyString(),
                        eq("id-1"),
                        eq("QA"),
                        eq("model"),
                        eq("hash"),
                        eq("ok"),
                        eq(1L),
                        eq(0),
                        eq(0),
                        eq(1),
                        eq(null));
    }

    @Test
    @DisplayName("失败调用 -> success=0 + error_msg")
    void record_failure() {
        recorder.record("QA", "OpenAiChatModel", "hash", null, 500L, 0, 0, false, "API timeout");

        verify(jdbcTemplate)
                .update(
                        anyString(),
                        eq("id-1"),
                        eq("QA"),
                        any(),
                        any(),
                        any(),
                        eq(500L),
                        any(),
                        any(),
                        eq(0),
                        eq("API timeout"));
    }

    @Test
    @DisplayName("超长 error_msg -> 截断到 512 字符")
    void record_truncatesLongError() {
        String longError = "e".repeat(1000);

        recorder.record("QA", "OpenAiChatModel", "hash", null, 1L, 0, 0, false, longError);

        verify(jdbcTemplate)
                .update(
                        anyString(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq("e".repeat(512)));
    }

    @Test
    @DisplayName("DB 写入失败 -> 吞掉异常，不影响主链路")
    void record_dbFailureSwallowed() {
        doThrow(new RuntimeException("db down")).when(jdbcTemplate).update(anyString(), any(Object[].class));

        assertThatCode(() -> recorder.record("QA", "model", "hash", "ok", 1L, 0, 0, true, null))
                .doesNotThrowAnyException();
    }
}
