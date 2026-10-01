package com.cartethyia.easyorange.ai.application.eval;

import com.cartethyia.easyorange.ai.domain.enums.RetrievalEvalLine;
import com.cartethyia.easyorange.ai.domain.model.RetrievalEvalCaseMetric;
import com.cartethyia.easyorange.ai.domain.model.RetrievalEvalRun;
import com.cartethyia.easyorange.ai.domain.port.RetrievalMetricQueryPort;
import com.cartethyia.easyorange.common.result.PageResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 检索质量回看服务 — ADR-0012「参数调整可基于历史数据回看」的读侧入口：批次趋势 + 用例级下钻。
 * <p>
 * 只读编排，不含任何计算口径：hit@5 与 MRR 的定义在 {@link RetrievalMetricQueryPort} 上声明，
 * 聚合由实现侧（JDBC 适配器）完成。这里做的是「回看」特有的两件事：
 * <p>
 * <b>① 不替空表编数字</b>：没有任何批次时返回空列表而不是造一条 hit@5=0 的批次 ——
 * 「还没跑过评测」与「跑了但全没命中」在面板上必须分得开，前者该提示去跑评测而不是显示 0%。
 * <p>
 * <b>② 下钻限定单批次</b>：{@code runId} 为空即回空页而不是列出全量用例 —— 全量明细跨批次混排时
 * reciprocal_rank 没有可比的基准，看的人会把它误读成「这些用例一直很差」。
 */
@Service
@RequiredArgsConstructor
public class RetrievalReviewAppService {

    /** 最近批次的默认条数：够看出趋势又不至于把首屏撑成一个长列表。 */
    public static final int DEFAULT_RECENT_LIMIT = 20;

    /** 下钻分页上限，与管理端其它列表页的 pageSize 上限一致。 */
    public static final int MAX_PAGE_SIZE = 100;

    private final RetrievalMetricQueryPort queryPort;

    /** 最近批次列表（按评测线过滤，时间倒序）。 */
    @Transactional(readOnly = true)
    public List<RetrievalEvalRun> recentRuns(RetrievalEvalLine line, int limit) {
        int capped = Math.min(Math.max(limit, 1), DEFAULT_RECENT_LIMIT);
        return queryPort.recentRuns(line, capped);
    }

    /** 单批次用例级明细；runId 为空返回空页（见类注释②）。 */
    @Transactional(readOnly = true)
    public PageResult<RetrievalEvalCaseMetric> pageCases(String runId, int pageNum, int pageSize) {
        if (runId == null || runId.isBlank()) {
            return PageResult.empty(Math.max(pageNum, 1), clampSize(pageSize));
        }
        return queryPort.pageCases(runId.trim(), Math.max(pageNum, 1), clampSize(pageSize));
    }

    private static int clampSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }
}
