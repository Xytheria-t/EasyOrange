package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.domain.port.AiCallLogPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 调用日志在 readOnly 事务内仍可落库 — 守卫 {@code AiCallLogRecorder} 的 {@code REQUIRES_NEW}。
 * <p>
 * 检索链路的入口事务是 {@code @Transactional(readOnly = true)}（搜索 → 向量化 → 记账），
 * 记录器若借用外层连接，INSERT 会被直接拒绝（{@code Connection is read-only}）——
 * SEMANTIC（搜索向量化）行曾因此一条都没进过成本报表，且每次请求打一条 WARN。
 * 本测试在只读事务里调用记录器，行落不下来即失败。
 */
@ActiveProfiles("it")
@DisplayName("AI 调用日志：外层只读事务内落库")
class AiCallLogRecorderIT extends AbstractIntegrationTest {

    private static final String SCOPE = "IT_READONLY";

    @Autowired
    private AiCallLogPort aiCallLogPort;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("readOnly 外层事务内 record 仍插入成功（REQUIRES_NEW 独立读写连接）")
    void recordInsideReadOnlyTransaction_stillInserts() {
        // prompt_hash char(32)：it-ro- + nanoTime 最长 25 字符，天然在定长内
        String promptHash = "it-ro-" + System.nanoTime();
        try {
            var readOnlyTx = new TransactionTemplate(transactionManager);
            readOnlyTx.setReadOnly(true);
            readOnlyTx.executeWithoutResult(
                    status -> aiCallLogPort.record(SCOPE, "it-model", promptHash, null, 1, 0, 0, true, null));

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM eo_ai_call_log WHERE prompt_hash = ?", Integer.class, promptHash);
            assertThat(count).as("只读外层事务内调用日志应由独立事务提交").isEqualTo(1);
        } finally {
            jdbcTemplate.update("DELETE FROM eo_ai_call_log WHERE prompt_hash = ?", promptHash);
        }
    }
}
