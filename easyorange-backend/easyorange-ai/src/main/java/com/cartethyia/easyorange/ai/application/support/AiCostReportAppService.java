package com.cartethyia.easyorange.ai.application.support;

import com.cartethyia.easyorange.ai.application.dto.AiCostReportRow;
import com.cartethyia.easyorange.ai.application.port.query.AiCostReportPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI 成本报表 — 按场景聚合 AI 调用日志的调用次数、token 用量、平均耗时与失败次数。
 * <p>
 * 本类只做「时间窗夹取」这一件治理决策：窗口是入参，合法区间不是。聚合口径与 SQL 在
 * {@link AiCostReportPort} 的实现里，应用层不碰持久化。
 */
@Service
@RequiredArgsConstructor
public class AiCostReportAppService {

    /** 时间窗上限 30 天：再宽的窗口在单表上就是全表扫描，且超出「近况」的语义。 */
    static final int MAX_WINDOW_HOURS = 24 * 30;

    private final AiCostReportPort costReportPort;

    public List<AiCostReportRow> report(int hours) {
        return costReportPort.report(clampWindow(hours));
    }

    static int clampWindow(int hours) {
        if (hours <= 0) {
            return 24;
        }
        return Math.min(hours, MAX_WINDOW_HOURS);
    }
}
