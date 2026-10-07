package com.cartethyia.easyorange.ai.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.application.dto.AiCostReportRow;
import com.cartethyia.easyorange.ai.application.support.AiCostReportAppService;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 成本报表（管理端）— 钱花在哪个「场景 × 模型」上，按单价表货币化。
 * /api/admin/** 由 SecurityConfig 统一限 ADMIN 角色。
 */
@Tag(name = "AI 报表", description = "AI 成本报表（按场景 × 模型，含货币化）")
@RestController
@RequestMapping("/api/admin/ai/cost-report")
@RequiredArgsConstructor
public class AdminCostReportController {

    private final AiCostReportAppService costReportService;

    @GetMapping
    public Result<List<AiCostReportRow>> costReport(@RequestParam(defaultValue = "24") int hours) {
        return Result.success(costReportService.report(hours));
    }
}
