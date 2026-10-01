package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.enums.RetrievalEvalLine;
import com.cartethyia.easyorange.ai.domain.model.RetrievalEvalCaseMetric;
import com.cartethyia.easyorange.ai.domain.model.RetrievalEvalRun;
import com.cartethyia.easyorange.common.result.PageResult;
import java.util.List;

/**
 * 检索指标回看端口 — 读 {@code eo_retrieval_metric}（写侧见 {@link RetrievalMetricPort}），兑现
 * ADR-0012「参数调整可基于历史数据回看」：调分块 / topK / RRF 的 k 之后，能拿历史批次对比而不是靠手感。
 * <p>
 * 批次口径在这里声明（分母 = 该 run 的用例数、按 case_id 前缀分评测线），实现侧只负责把它翻译成聚合 SQL。
 */
public interface RetrievalMetricQueryPort {

    /** 最近 {@code limit} 个批次，按采样时间倒序；同一 run 跨评测线时按前缀拆行。 */
    List<RetrievalEvalRun> recentRuns(RetrievalEvalLine line, int limit);

    /** 单个批次的用例级明细（分页），按命中与否升序 —— 未命中的排前面，回看时先看到问题用例。 */
    PageResult<RetrievalEvalCaseMetric> pageCases(String runId, int pageNum, int pageSize);
}
