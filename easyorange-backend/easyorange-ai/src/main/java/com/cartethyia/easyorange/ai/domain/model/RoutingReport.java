package com.cartethyia.easyorange.ai.domain.model;

/**
 * 路由质量回归报告 — 金标准集 {@code expected_tools} 的命中汇总。
 * <p>
 * 与生成质量报告互补而非重复：答案能靠知识库兜底答对，路由走错这件事只有独立看工具路径才量得到。
 *
 * @param totalCases  参与路由评估的用例数（标了 expected_tools 的 chat 用例；跑失败的也计入分母）
 * @param correctCases 期望工具全部出现在实际路径里的用例数
 * @param accuracy    准确率（0-1）
 */
public record RoutingReport(int totalCases, int correctCases, double accuracy) {}
