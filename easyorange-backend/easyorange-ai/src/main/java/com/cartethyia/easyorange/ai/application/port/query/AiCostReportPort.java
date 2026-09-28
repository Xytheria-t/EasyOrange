package com.cartethyia.easyorange.ai.application.port.query;

import com.cartethyia.easyorange.ai.application.dto.AiCostReportRow;
import java.util.List;

/**
 * AI 成本报表读端口 — 按场景聚合 {@code eo_ai_call_log} 的调用次数、token 用量、平均耗时与失败次数。
 * <p>
 * 读模型是应用层概念（不是领域契约），所以端口落在 application 而不是 domain。
 * <p>
 * <b>只统计供应商真实回报的用量</b>：未带 usage 的调用（部分兼容端点忽略 {@code stream_options}）一律记 0，
 * 不在这里估算。估算口径留给预算器（它按场景上限兜底是为了让日限额不被绕过），
 * 但成本报表里混入估算值会让人把「没测到」当成「不花钱」——比缺数据更危险。
 */
public interface AiCostReportPort {

    /**
     * 按场景聚合出一行一场景的成本报表。
     *
     * @param hours 时间窗（小时），已由调用方夹到合法区间
     */
    List<AiCostReportRow> report(int hours);
}
