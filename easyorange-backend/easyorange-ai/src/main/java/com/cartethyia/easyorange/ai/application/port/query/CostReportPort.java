package com.cartethyia.easyorange.ai.application.port.query;

import com.cartethyia.easyorange.ai.application.dto.CostReportRow;
import java.util.List;

/**
 * AI 成本报表读端口 — 按「场景 × 模型」聚合 {@code eo_ai_call_log} 的调用次数、token、平均耗时与失败次数。
 * <p>
 * 端口落 application 而非 domain：读模型是应用层概念，不是领域契约。
 * <p>
 * <b>只统计供应商真实回报的用量</b>：未带 usage 的调用（部分兼容端点忽略 {@code stream_options}）记 0，不估算
 * —— 混入估算值会让人把「没测到」当成「不花钱」，比缺数据更危险；估算只归预算器（按场景上限兜底防绕过日限额）。
 */
public interface CostReportPort {

    /** 一行一「场景 × 模型」组合；@param hours 时间窗（小时），已由调用方夹到合法区间 */
    List<CostReportRow> report(int hours);
}
