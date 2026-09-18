package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.application.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.application.service.AutoListingService;
import com.cartethyia.easyorange.common.annotation.SkipRateLimit;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 上架辅助端点 — 拍照识别（Vision）。
 * <p>
 * 只留一个入口：识别结果已经填满上架表单要的字段（属性 / 建议价 / 标题 / 描述），
 * 独立的「智能估值」与「文案生成」是对同一批产出的重复入口、各自还要多付一次模型调用，
 * 已于 2026-09-19 一并删除。
 * <p>
 * 商品审核不在这里：审核建议由管理端按需触发（{@code GET /api/admin/products/{id}/ai-review}），
 * 卖家侧没有入口。
 */
@SkipRateLimit
@Tag(name = "AI 服务", description = "AI 上架辅助：拍照识别")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiListingController {

    private final AutoListingService autoListingService;

    @PostMapping("/auto-listing")
    public Result<AutoListingResult> autoListing(@RequestBody List<String> imageUrls) {
        return Result.success(autoListingService.analyzeImages(imageUrls));
    }
}
