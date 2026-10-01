package com.cartethyia.easyorange.adapter.inbound.web.response;

/**
 * 检索评测批次视图 — 管理端批次列表行。
 *
 * @param lineLabel 评测线中文名（知识库检索 / 找货检索）
 * @param hitRatePct hit@5 的百分比形式（管理端展示口径，原始比例仍在 domain 侧）
 */
public record RetrievalEvalRunVO(
        String runId,
        String line,
        String lineLabel,
        int caseCount,
        int hitCount,
        double hitRateAt5,
        double hitRatePct,
        double mrr,
        String createdAt) {}
