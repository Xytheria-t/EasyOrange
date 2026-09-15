package com.cartethyia.easyorange.ai.domain.port;

/**
 * 反馈导出端口 — 把未导出的用户反馈渲染成金标准集用例片段，导出即标记。
 * <p>
 * 实现方在 adapter/outbound（{@code eo_ai_feedback} 表）；供管理端导出端点消费，
 * 人工审核后合入 {@code eval/golden-set.yaml}。
 */
public interface GoldenSetExportPort {

    String exportUnreviewed(int limit);
}
