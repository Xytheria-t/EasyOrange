package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.adapter.outbound.persistence.GoldenSetExportService;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 反馈 → 金标准评测集导出（管理端）— 把用户 👍/👎 反馈渲染成 golden-set.yaml 用例片段，
 * 导出即标记 exported=1；人工审核后合入 eval/golden-set.yaml，实现「反馈飞轮自动扩充评测集」。
 */
@Tag(name = "平台运维", description = "AI 反馈导出为金标准评测集用例")
@RestController
@RequestMapping("/api/admin/ai/feedback")
@RequiredArgsConstructor
public class AdminFeedbackExportController {

    private final GoldenSetExportService exportService;

    @GetMapping("/export")
    public Result<String> export(@RequestParam(defaultValue = "50") int limit) {
        return Result.success(exportService.exportUnreviewed(limit));
    }
}
