package com.cartethyia.easyorange.ai.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.ai.adapter.inbound.web.dto.response.RetrievalEvalCaseVO;
import com.cartethyia.easyorange.ai.adapter.inbound.web.dto.response.RetrievalEvalRunVO;
import com.cartethyia.easyorange.ai.domain.model.RetrievalEvalCaseMetric;
import com.cartethyia.easyorange.ai.domain.model.RetrievalEvalRun;
import com.cartethyia.easyorange.common.result.PageResult;
import java.util.List;

/**
 * 检索质量回看视图组装 — domain 模型 → 对外 VO。
 * <p>
 * hit@5 的百分比形式在这里出：domain 侧保留 0-1 比例供程序比较，管理端展示要的是百分数，
 * 两份口径同时外发会让看的人以为「0.90 与 90% 是两个指标」。
 */
public final class RetrievalEvalAssembler {

    private RetrievalEvalAssembler() {}

    public static List<RetrievalEvalRunVO> toVOs(List<RetrievalEvalRun> runs) {
        return runs.stream().map(RetrievalEvalAssembler::toVO).toList();
    }

    public static RetrievalEvalRunVO toVO(RetrievalEvalRun run) {
        return new RetrievalEvalRunVO(
                run.runId(),
                run.line().name(),
                run.line().label(),
                run.caseCount(),
                run.hitCount(),
                run.hitRateAt5(),
                run.hitRateAt5() * 100,
                run.mrr(),
                run.createdAt());
    }

    public static PageResult<RetrievalEvalCaseVO> toCasePage(PageResult<RetrievalEvalCaseMetric> page) {
        return new PageResult<>(
                page.records().stream().map(RetrievalEvalAssembler::toCaseVO).toList(),
                page.total(),
                page.current(),
                page.size(),
                page.pages());
    }

    public static RetrievalEvalCaseVO toCaseVO(RetrievalEvalCaseMetric metric) {
        return new RetrievalEvalCaseVO(
                metric.caseId(),
                metric.queryText(),
                metric.goldDocIds(),
                metric.hitAt5(),
                metric.reciprocalRank(),
                metric.hitRank());
    }
}
