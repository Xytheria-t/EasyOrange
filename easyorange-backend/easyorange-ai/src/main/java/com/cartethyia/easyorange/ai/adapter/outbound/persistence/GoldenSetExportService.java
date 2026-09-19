package com.cartethyia.easyorange.ai.adapter.outbound.persistence;

import com.cartethyia.easyorange.ai.domain.port.GoldenSetExportPort;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 反馈 → 金标准评测集导出 — 把可用的用户反馈渲染成 golden-set.yaml 用例片段，
 * 导出即标记 exported=1；人工审核后合入 {@code eval/golden-set.yaml}，实现「反馈飞轮自动扩充评测集」。
 * <p>
 * <b>只导「能自动成用例」的反馈</b>（{@link #EXPORTABLE}）：金标准集加载即校验
 * （{@code GoldenSetLoader.validate}），chat 用例必须有非空 reference_answer、retrieval 用例必须有
 * gold_doc_ids —— 从反馈里能自动拿到的只有前者，因此：
 * <ul>
 *   <li><b>helpful = 0（点踩）不能自动成用例</b>：反馈里存的 response_text 正是被用户嫌弃的那条回答，
 *       拿它当 reference_answer 等于把错答案钉成标准，下一轮评测会把「答得对」判成回归。
 *       负样本必须先由人工补一条正确回答，故只统计不导出。</li>
 *   <li><b>scope != chat</b>：反馈里没有 gold_doc_ids，导出的片段必然过不了加载校验。</li>
 *   <li><b>问题或回答为空</b>：导出的用例缺字段，加载期就会炸。</li>
 * </ul>
 * 不能自动成用例的行<b>不标 exported</b>：标了就等于「已产出用例」，下次导出不再提示，这条信号就丢了。
 * 它们只在片段头部按条数汇总提示人工处理，并保持 exported = 0 —— 与幂等处理同一个原则：宁可反复提示，不可漏处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoldenSetExportService implements GoldenSetExportPort {

    /**
     * 「能自动成用例」的唯一判据 —— 正取（导出）与取反（统计待人工）共用同一段 SQL，
     * 不会出现「导出认为可用、统计认为不可用」的两套口径。
     */
    private static final String EXPORTABLE = "helpful = 1 AND scope = 'chat'"
            + " AND COALESCE(query_text, '') <> '' AND COALESCE(response_text, '') <> ''";

    private static final String SELECT_SQL =
            "SELECT id, query_text, response_text FROM eo_ai_feedback WHERE exported = 0 AND " + EXPORTABLE
                    + " ORDER BY created_at LIMIT ?";

    private static final String COUNT_NEEDS_MANUAL_SQL =
            "SELECT COUNT(*) FROM eo_ai_feedback WHERE exported = 0 AND NOT (" + EXPORTABLE + ")";

    private static final String MARK_EXPORTED_SQL = "UPDATE eo_ai_feedback SET exported = 1 WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String exportUnreviewed(int limit) {
        List<Map<String, Object>> rows;
        int needsManual;
        try {
            rows = jdbcTemplate.queryForList(SELECT_SQL, limit);
            needsManual = countNeedsManual();
        } catch (Exception e) {
            log.warn("Export AI feedback failed", e);
            return "# 导出失败，请检查数据库连接";
        }

        var cases = new StringBuilder();
        for (Map<String, Object> row : rows) {
            String id = String.valueOf(row.get("id"));
            // 缩进与 golden-set.yaml 的 cases: 列表项一致，人工审核后可直接粘贴合入
            cases.append("  - id: fb-").append(shortId(id)).append('\n');
            cases.append("    scope: chat\n");
            cases.append("    question: ").append(quote(row.get("query_text"))).append('\n');
            cases.append("    reference_answer: ")
                    .append(quote(row.get("response_text")))
                    .append('\n');
            jdbcTemplate.update(MARK_EXPORTED_SQL, id);
        }

        if (cases.isEmpty() && needsManual == 0) {
            return "# 暂无新的用户反馈可导出";
        }

        var yaml = new StringBuilder();
        if (needsManual > 0) {
            yaml.append("# 另有 ")
                    .append(needsManual)
                    .append(" 条未导出反馈不能自动成用例（点踩需人工补正确回答；非 chat 场景需人工补 gold_doc_ids；字段为空需人工补全），\n")
                    .append("# 未标记 exported，可用 SELECT ... FROM eo_ai_feedback WHERE exported = 0 复核\n");
        }
        if (!cases.isEmpty()) {
            yaml.append("# 以下片段按 golden-set.yaml 的缩进渲染，可直接粘到 cases: 下（人工审核后合并）\n");
            yaml.append(cases);
        }
        return yaml.toString();
    }

    private int countNeedsManual() {
        Integer count = jdbcTemplate.queryForObject(COUNT_NEEDS_MANUAL_SQL, Integer.class);
        return count == null ? 0 : count;
    }

    private static String shortId(String id) {
        return id.substring(0, Math.min(8, id.length()));
    }

    /**
     * YAML 双引号标量 —— 一律加引号，避免问题/回答里的 {@code ": "}、前导特殊字符把片段变成非法 YAML。
     * <p>
     * 反斜杠必须<b>最先</b>转义：先转 {@code \"} 再转 {@code \\} 会把刚插入的转义符二次转义，
     * 解析出来会多一个反斜杠。换行统一收敛成 {@code \n}（片段里一条用例占一行，便于人工 diff）。
     */
    private static String quote(Object value) {
        String text =
                value == null ? "" : String.valueOf(value).replace("\r\n", "\n").replace('\r', '\n');
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return '"' + escaped + '"';
    }
}
