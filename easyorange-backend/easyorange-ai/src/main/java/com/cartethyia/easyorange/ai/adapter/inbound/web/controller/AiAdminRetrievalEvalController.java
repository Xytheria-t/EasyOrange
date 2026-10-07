package com.cartethyia.easyorange.ai.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.adapter.inbound.web.assembler.RetrievalEvalAssembler;
import com.cartethyia.easyorange.ai.adapter.inbound.web.dto.response.RetrievalEvalCaseVO;
import com.cartethyia.easyorange.ai.adapter.inbound.web.dto.response.RetrievalEvalRunVO;
import com.cartethyia.easyorange.ai.application.eval.RetrievalReviewAppService;
import com.cartethyia.easyorange.ai.domain.enums.RetrievalEvalLine;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端检索质量回看 — 读 {@code eo_retrieval_metric} 的历史批次与用例明细，兑现 ADR-0012
 * 「参数调整可基于历史数据回看」：调分块 / topK / RRF 的 k 之后在这里比对，而不是靠手感。
 * <p>
 * 路径落在 {@code /api/admin/**} 下，由安全配置统一限 ADMIN（见 SecurityConfig 管理后台规则），
 * 不额外标 {@code @PreAuthorize}。
 * <p>
 * 口径提示（管理端页面同源展示）：hit@5 与 MRR 只统计<b>融合腿（生产路径）</b>，单腿属消融日志不落表；
 * 两条评测线（retr-* 真实 embedding / asset-* 合成向量）语料空间不同，必须分列比较。
 */
@Tag(name = "管理后台-AI 检索评测", description = "检索质量回看（评测批次趋势 + 用例级明细）")
@RestController
@RequestMapping("/api/admin/ai/retrieval-eval")
@RequiredArgsConstructor
public class AiAdminRetrievalEvalController {

    private final RetrievalReviewAppService reviewService;

    @GetMapping("/runs")
    @Operation(summary = "最近评测批次（hit@5 / MRR 趋势）")
    public Result<List<RetrievalEvalRunVO>> listRuns(
            @RequestParam(required = false) RetrievalEvalLine line, @RequestParam(defaultValue = "20") int limit) {
        RetrievalEvalLine target = line == null ? RetrievalEvalLine.KNOWLEDGE : line;
        return Result.success(RetrievalEvalAssembler.toVOs(reviewService.recentRuns(target, limit)));
    }

    @GetMapping("/cases")
    @Operation(summary = "单批次用例级明细（未命中排前）")
    public Result<PageResult<RetrievalEvalCaseVO>> listCases(
            @RequestParam String runId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(RetrievalEvalAssembler.toCasePage(reviewService.pageCases(runId, pageNum, pageSize)));
    }
}
