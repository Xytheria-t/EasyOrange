package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.application.dto.AiCostReportRow;
import com.cartethyia.easyorange.ai.application.service.AiCostReportService;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 成本报表端点（管理端）— 按场景看「谁在花 token」。
 * <p>
 * 路径落在 {@code /api/admin/**} 下，由安全配置统一限 ADMIN/MANAGER（见 SecurityConfig 管理后台规则），
 * 不额外标 {@code @PreAuthorize}。
 */
@Tag(name = "AI 成本报表", description = "按场景聚合的 AI 调用次数 / token 用量 / 平均耗时 / 失败次数")
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AiCostReportController {

    private final AiCostReportService costReportService;

    @GetMapping("/cost-report")
    public Result<List<AiCostReportRow>> costReport(@RequestParam(defaultValue = "24") int hours) {
        return Result.success(costReportService.report(hours));
    }
}
