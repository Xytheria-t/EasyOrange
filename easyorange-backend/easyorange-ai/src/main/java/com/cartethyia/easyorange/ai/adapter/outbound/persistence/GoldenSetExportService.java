package com.cartethyia.easyorange.ai.adapter.outbound.persistence;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 反馈 → 金标准评测集导出 — 把未导出的用户反馈（exported=0）渲染成 golden-set.yaml 用例片段，
 * 导出即标记；人工审核后合入 {@code eval/golden-set.yaml}，实现「反馈飞轮自动扩充评测集」。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoldenSetExportService {

    private static final String SELECT_SQL =
            "SELECT id, scope, query_text, response_text FROM eo_ai_feedback WHERE exported = 0 ORDER BY created_at LIMIT ?";

    private static final String MARK_EXPORTED_SQL = "UPDATE eo_ai_feedback SET exported = 1 WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    public String exportUnreviewed(int limit) {
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(SELECT_SQL, limit);
        } catch (Exception e) {
            log.warn("Export AI feedback failed", e);
            return "# 导出失败，请检查数据库连接";
        }
        if (rows.isEmpty()) {
            return "# 暂无新的用户反馈可导出";
        }
        var yaml = new StringBuilder("# 自动导出：用户反馈 → 金标准用例（人工审核后合入 eval/golden-set.yaml）\n");
        for (Map<String, Object> row : rows) {
            String id = (String) row.get("id");
            String scope = (String) row.get("scope");
            String question = (String) row.get("query_text");
            String answer = (String) row.get("response_text");
            yaml.append("- id: fb-")
                    .append(id.substring(0, Math.min(8, id.length())))
                    .append('\n');
            yaml.append("  scope: ")
                    .append(scope == null ? "chat" : scope.toLowerCase())
                    .append('\n');
            yaml.append("  question: ").append(singleLine(question)).append('\n');
            yaml.append("  reference_answer: ").append(singleLine(answer)).append('\n');
            jdbcTemplate.update(MARK_EXPORTED_SQL, id);
        }
        return yaml.toString();
    }

    private static String singleLine(String text) {
        if (text == null || text.isBlank()) {
            return "\"\"";
        }
        return text.replace('\n', ' ').replace('\r', ' ').replace("\"", "\\\"");
    }
}
