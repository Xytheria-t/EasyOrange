package com.cartethyia.easyorange.ai.domain.model;

/**
 * 单条检索用例的采样明细 — 管理端下钻用，回答「这批为什么是这个分」。
 * <p>
 * 逐条明细是回看面的真正价值：批次级 hit@5 掉了要能立刻定位到是哪几条用例、它们的查询长什么样、
 * gold 是谁。缺了明细只剩一条数字，参数调整就变成盲调。
 *
 * @param caseId         用例 ID（retr-* 或 asset-*，前缀即评测线）
 * @param queryText      检索查询原文
 * @param goldDocIds     期望命中文档 ID（逗号分隔，原样透传不做拆分）
 * @param hitAt5         top-5 是否命中
 * @param reciprocalRank 首个命中位置的倒数（未命中 0；0 < rr ≤ 1，rr = 1 即排第一）
 */
public record RetrievalEvalCaseMetric(
        String caseId, String queryText, String goldDocIds, boolean hitAt5, double reciprocalRank) {

    /** 命中位次 = 1 / rr（未命中无位次返回 null）—— 面板直接显示「第 N 位」比显示 0.25 可读。 */
    public Integer hitRank() {
        if (reciprocalRank <= 0) {
            return null;
        }
        return (int) Math.round(1 / reciprocalRank);
    }
}
