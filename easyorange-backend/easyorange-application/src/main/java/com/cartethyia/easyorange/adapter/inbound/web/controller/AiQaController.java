package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.application.dto.QaRequest;
import com.cartethyia.easyorange.ai.application.dto.QaResponse;
import com.cartethyia.easyorange.ai.application.service.AiQaService;
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
 * AI 客服问答端点 — 商品问答（LLM 生成）。
 * <p>
 * 原 {@code GET /api/ai/semantic-search} 已删除：语义召回归并入商品搜索
 * （{@code GET /api/products/search} 内部 BM25 + kNN 两路召回、RRF 融合），
 * 不再对外单独暴露一路召回让用户自己挑。见 ADR-0012。
 */
@SkipRateLimit
@Tag(name = "AI 服务", description = "AI 客服问答：商品问答")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiQaController {

    private final AiQaService qaService;

    @PostMapping("/qa")
    public Result<QaResponse> answerQuestion(@Valid @RequestBody QaRequest request) {
        return Result.success(qaService.answerQuestion(request));
    }
}
