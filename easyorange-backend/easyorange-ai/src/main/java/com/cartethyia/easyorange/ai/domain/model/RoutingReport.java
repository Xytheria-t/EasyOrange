package com.cartethyia.easyorange.ai.domain.model;

/** 路由质量回归报告 — expected_tools 命中汇总。totalCases 是标了 expected_tools 的 chat 用例数、跑失败的也计入分母；与生成分数互补：答案能靠知识库兜底答对，路由走错只有独立看工具路径才量得到。 */
public record RoutingReport(int totalCases, int correctCases, double accuracy) {}
