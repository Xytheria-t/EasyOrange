package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.application.dto.AiCostReportRow;
import com.cartethyia.easyorange.ai.application.support.AiCostReportService;
import com.cartethyia.easyorange.ai.domain.model.AiListingAdoptionReport;
import com.cartethyia.easyorange.ai.domain.port.AiListingAdoptionPort;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 管理端报表端点 — 成本（钱花在哪）+ 效果（建议有没有被采信）。
 * <p>
 * 两边合起来才完整：成本报表说「谁在花 token」，采纳率说「花出去的 token 有没有用」。
 * 路径落在 {@code /api/admin/**} 下，由安全配置统一限 ADMIN/MANAGER（见 SecurityConfig 管理后台规则），
 * 不额外标 {@code @PreAuthorize}。
 */
@Tag(name = "AI 报表", description = "AI 成本报表（按场景）与 AI 建议字段级采纳率")
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AiCostReportController {

    private final AiCostReportService costReportService;
    private final AiListingAdoptionPort listingAdoptionPort;

    @GetMapping("/cost-report")
    public Result<List<AiCostReportRow>> costReport(@RequestParam(defaultValue = "24") int hours) {
        return Result.success(costReportService.report(hours));
    }

    @GetMapping("/listing-adoption")
    public Result<AiListingAdoptionReport> listingAdoption() {
        return Result.success(listingAdoptionPort.report());
    }
}
