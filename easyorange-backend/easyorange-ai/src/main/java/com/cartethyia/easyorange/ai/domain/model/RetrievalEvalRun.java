package com.cartethyia.easyorange.ai.domain.model;

import com.cartethyia.easyorange.ai.domain.enums.RetrievalEvalLine;

/**
 * 一次评测批次（run_id）的检索质量汇总 — 管理端「检索质量回看」的批次行。hit@5 与 MRR 是
 * 本批逐用例 reciprocal_rank 的聚合；批次间比较的是两次独立采样，因此不做跨 run 累加。
 *
 * @param line       评测线（按 case_id 前缀判定，见 {@link RetrievalEvalLine}）
 * @param caseCount  本批用例数（hit@5 与 MRR 的共同分母）
 * @param hitRateAt5 hit@5 = hitCount / caseCount
 * @param mrr        SUM(reciprocal_rank) / caseCount
 * @param createdAt  批次采样时间（取同批最早一条代表整批）
 */
public record RetrievalEvalRun(
        String runId,
        RetrievalEvalLine line,
        int caseCount,
        int hitCount,
        double hitRateAt5,
        double mrr,
        String createdAt) {}
