package com.cartethyia.easyorange.ai.eval;

/**
 * 生成质量回归报告 — 金标准集 Judge 打分的汇总。
 *
 * @param totalCases  用例总数
 * @param judgedCases 成功完成评分的用例数（模型/评审异常会跳过）
 * @param avgScore    平均分（1-5）
 */
public record GenerationReport(int totalCases, int judgedCases, double avgScore) {}
