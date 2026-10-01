package com.cartethyia.easyorange.ai.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 用例明细模型测试 — 命中位次换算是面板唯一的计算：rr 是倒数，显示「第 0 位」或「第 6 位」都算错。
 */
@DisplayName("检索用例明细模型 -> 测试")
class RetrievalEvalCaseMetricTest {

    @Test
    @DisplayName("rr = 1（排第一）-> 位次 1")
    void hitRank_firstPosition() {
        assertThat(new RetrievalEvalCaseMetric("retr-001", "q", "kb-0001", true, 1.0).hitRank())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rr = 0.25 -> 位次 4")
    void hitRank_fourthPosition() {
        assertThat(new RetrievalEvalCaseMetric("retr-002", "q", "kb-0002", true, 0.25).hitRank())
                .isEqualTo(4);
    }

    @Test
    @DisplayName("rr = 0（未命中）-> 位次为 null，面板显示「未命中」而不是「第 0 位」")
    void hitRank_unhitReturnsNull() {
        assertThat(new RetrievalEvalCaseMetric("retr-003", "q", "kb-0003", false, 0).hitRank())
                .isNull();
    }
}
