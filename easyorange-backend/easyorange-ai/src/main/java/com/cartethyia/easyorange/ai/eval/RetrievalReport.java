package com.cartethyia.easyorange.ai.eval;

/**
 * 检索质量回归报告 — 金标准集 hit@5 / MRR 的汇总（回答「RAG 检索层好不好」的量化答案）。
 */
public record RetrievalReport(int totalCases, int hitCases, double hitRateAt5, double mrr) {}
