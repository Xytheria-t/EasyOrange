package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.dto.CopyGenerationRequest;
import com.cartethyia.easyorange.ai.dto.CopyGenerationResult;
import com.cartethyia.easyorange.ai.service.AiCopyGenerationService;
import com.cartethyia.easyorange.common.annotation.SkipRateLimit;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 营销文案端点 — 按商品信息生成标题 + 描述（style 缺省 standard）。
 */
@SkipRateLimit
@Tag(name = "AI 服务", description = "AI 营销文案生成")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiCopyController {

    private final AiCopyGenerationService copyGenerationService;

    @PostMapping("/generate-copy")
    public Result<CopyGenerationResult> generateCopy(@Valid @RequestBody CopyGenerationRequest request) {
        return Result.success(copyGenerationService.generateCopy(
                request.productName(),
                request.categoryName(),
                request.conditionLevel(),
                request.originalPrice(),
                request.style()));
    }
}
