package com.cartethyia.easyorange.ai.service;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.adapter.outbound.persistence.GoldenSetExportService;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("反馈飞轮 -> 测试")
class FeedbackLoopTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private IdGenerator idGenerator;

    @Test
    @DisplayName("反馈入库 -> 8 参 INSERT（含用户 ID）")
    void recordFeedback() {
        when(idGenerator.generateId()).thenReturn("fb-1");
        AiFeedbackService service = new AiFeedbackService(jdbcTemplate, idGenerator);

        service.record("chat", "怎么退款？", "7 天无理由", true, "很实用", "log-1");

        verify(jdbcTemplate)
                .update(
                        anyString(),
                        eq("fb-1"),
                        eq("chat"),
                        eq("怎么退款？"),
                        eq("7 天无理由"),
                        eq(1),
                        eq("很实用"),
                        eq("log-1"),
                        isNull());
    }

    @Test
    @DisplayName("反馈入库失败 -> 只告警不抛出")
    void recordFeedback_swallowsError() {
        when(idGenerator.generateId()).thenReturn("fb-1");
        when(jdbcTemplate.update(anyString(), org.mockito.ArgumentMatchers.<Object>any()))
                .thenThrow(new RuntimeException("db down"));
        AiFeedbackService service = new AiFeedbackService(jdbcTemplate, idGenerator);

        service.record("chat", "问题", "回答", false, null, null);
    }

    @Test
    @DisplayName("导出未评审反馈 -> 渲染 YAML 片段并标记 exported")
    void exportUnreviewed() {
        when(jdbcTemplate.queryForList(anyString(), anyInt()))
                .thenReturn(List.of(Map.of(
                        "id", "feedback-001",
                        "scope", "chat",
                        "query_text", "怎么退款？",
                        "response_text", "7 天内可无理由退货")));
        GoldenSetExportService service = new GoldenSetExportService(jdbcTemplate);

        String yaml = service.exportUnreviewed(50);

        org.assertj.core.api.Assertions.assertThat(yaml)
                .contains("- id: fb-feedback")
                .contains("question: 怎么退款？")
                .contains("reference_answer: 7 天内可无理由退货");
        verify(jdbcTemplate).update(anyString(), eq("feedback-001"));
    }

    @Test
    @DisplayName("无未导出反馈 -> 提示文案，不查询更新")
    void exportUnreviewed_empty() {
        when(jdbcTemplate.queryForList(anyString(), anyInt())).thenReturn(List.of());
        GoldenSetExportService service = new GoldenSetExportService(jdbcTemplate);

        String yaml = service.exportUnreviewed(50);

        org.assertj.core.api.Assertions.assertThat(yaml).contains("暂无");
        verify(jdbcTemplate, never()).update(anyString(), org.mockito.ArgumentMatchers.any(Object[].class));
    }
}
