package com.cartethyia.easyorange.ai.domain.model;

/** 检索质量回归报告 — 金标准集 hit@5 / MRR 的汇总。 */
public record RetrievalReport(int totalCases, int hitCases, double hitRateAt5, double mrr) {}
