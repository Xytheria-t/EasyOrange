package com.cartethyia.easyorange.ai.domain.model;

import com.cartethyia.easyorange.ai.domain.enums.RetrievalEvalLine;

/**
 * 一次评测批次（run_id）的检索质量汇总 — 管理端「检索质量回看」的批次行。
 * <p>
 * hit@5 与 MRR 都是本批逐用例 reciprocal_rank 的聚合。一次 run 就是一批口径相同的样本，
 * 批次间比较看的是两次独立采样的结果，因此不做跨 run 累加。
 *
 * @param runId      评测批次 ID
 * @param line       评测线（按 case_id 前缀判定，见 {@link RetrievalEvalLine}）
 * @param caseCount  本批用例数（hit@5 与 MRR 的共同分母）
 * @param hitCount   命中用例数
 * @param hitRateAt5 hit@5 = hitCount / caseCount
 * @param mrr        平均倒数排名 = SUM(reciprocal_rank) / caseCount
 * @param createdAt  批次采样时间（同批写入时间接近，取最早一条代表整批）
 */
public record RetrievalEvalRun(
        String runId,
        RetrievalEvalLine line,
        int caseCount,
        int hitCount,
        double hitRateAt5,
        double mrr,
        String createdAt) {}
