package com.cartethyia.easyorange.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.yaml.snakeyaml.Yaml;

@ExtendWith(MockitoExtension.class)
@DisplayName("反馈飞轮 -> 测试")
class FeedbackLoopTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private IdGenerator idGenerator;

    @Nested
    @DisplayName("反馈入库")
    class RecordFeedback {

        @Test
        @DisplayName("8 参 INSERT（含用户 ID）")
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
        @DisplayName("入库失败 -> 只告警不抛出")
        void recordFeedback_swallowsError() {
            when(idGenerator.generateId()).thenReturn("fb-1");
            when(jdbcTemplate.update(anyString(), org.mockito.ArgumentMatchers.<Object>any()))
                    .thenThrow(new RuntimeException("db down"));
            AiFeedbackService service = new AiFeedbackService(jdbcTemplate, idGenerator);

            service.record("chat", "问题", "回答", false, null, null);
        }
    }

    @Nested
    @DisplayName("导出为金标准用例")
    class ExportUnreviewed {

        @Test
        @DisplayName("好评反馈 -> 渲染成可解析的用例片段并标记 exported")
        void exportUnreviewed() {
            stubRows(Map.of("id", "feedback-001", "query_text", "怎么退款？", "response_text", "7 天内可无理由退货"));
            GoldenSetExportService service = new GoldenSetExportService(jdbcTemplate);

            String yaml = service.exportUnreviewed(50);

            assertThat(yaml).contains("  - id: fb-feedback");
            verify(jdbcTemplate).update(anyString(), eq("feedback-001"));
            // 片段按 golden-set.yaml 的 cases: 缩进渲染，直接粘上去就能被 GoldenSetLoader 解析
            Map<String, Object> case0 = parseFirstCase(yaml);
            assertThat(case0.get("scope")).isEqualTo("chat");
            assertThat(case0.get("question")).isEqualTo("怎么退款？");
            assertThat(case0.get("reference_answer")).isEqualTo("7 天内可无理由退货");
        }

        @Test
        @DisplayName("含引号/反斜杠/换行的反馈 -> 转义后仍能原样解析回来")
        void exportUnreviewed_escapesSpecialChars() {
            String question = "这个\"引号\"和\\反斜杠\n换行 怎么办？";
            String answer = "前面有冒号: 后面也有\t制表符";
            stubRows(Map.of("id", "feedback-002", "query_text", question, "response_text", answer));
            GoldenSetExportService service = new GoldenSetExportService(jdbcTemplate);

            Map<String, Object> case0 = parseFirstCase(service.exportUnreviewed(50));

            assertThat(case0.get("question")).isEqualTo(question);
            assertThat(case0.get("reference_answer")).isEqualTo(answer);
        }

        @Test
        @DisplayName("存在不能自动成用例的反馈 -> 头部提示条数，且不标记 exported")
        void exportUnreviewed_reportsNeedsManual() {
            stubRows(Map.of("id", "feedback-003", "query_text", "问题", "response_text", "回答"));
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(2);
            GoldenSetExportService service = new GoldenSetExportService(jdbcTemplate);

            String yaml = service.exportUnreviewed(50);

            assertThat(yaml).contains("另有 2 条未导出反馈不能自动成用例");
            assertThat(yaml).contains("未标记 exported");
            verify(jdbcTemplate, never()).update(anyString(), eq("feedback-004"));
        }

        @Test
        @DisplayName("没有可用反馈但有待人工的 -> 只给提示，不说「暂无」")
        void exportUnreviewed_onlyNeedsManual() {
            when(jdbcTemplate.queryForList(anyString(), anyInt())).thenReturn(List.of());
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
            GoldenSetExportService service = new GoldenSetExportService(jdbcTemplate);

            String yaml = service.exportUnreviewed(50);

            assertThat(yaml).doesNotContain("暂无").contains("另有 1 条");
        }

        @Test
        @DisplayName("无未导出反馈 -> 提示文案，不查询更新")
        void exportUnreviewed_empty() {
            when(jdbcTemplate.queryForList(anyString(), anyInt())).thenReturn(List.of());
            GoldenSetExportService service = new GoldenSetExportService(jdbcTemplate);

            String yaml = service.exportUnreviewed(50);

            assertThat(yaml).contains("暂无");
            verify(jdbcTemplate, never()).update(anyString(), org.mockito.ArgumentMatchers.any(Object[].class));
        }

        @Test
        @DisplayName("查询失败 -> 返回提示文案，不抛出")
        void exportUnreviewed_queryFails() {
            when(jdbcTemplate.queryForList(anyString(), anyInt())).thenThrow(new RuntimeException("db down"));
            GoldenSetExportService service = new GoldenSetExportService(jdbcTemplate);

            assertThat(service.exportUnreviewed(50)).contains("导出失败");
        }

        /** 把片段当 {@code cases:} 的子节点解析（与人工合并进 golden-set.yaml 的形态一致）。 */
        @SuppressWarnings("unchecked")
        private static Map<String, Object> parseFirstCase(String fragment) {
            Map<String, Object> root = new Yaml().load("cases:\n" + fragment);
            List<Map<String, Object>> cases = (List<Map<String, Object>>) root.get("cases");
            assertThat(cases).as("导出片段必须能作为 cases 列表解析").hasSize(1);
            return cases.getFirst();
        }

        private void stubRows(Map<String, Object> row) {
            when(jdbcTemplate.queryForList(anyString(), anyInt())).thenReturn(List.of(row));
        }
    }
}
