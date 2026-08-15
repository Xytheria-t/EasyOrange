package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.dto.QaRequest;
import com.cartethyia.easyorange.ai.dto.QaResponse;
import com.cartethyia.easyorange.ai.dto.SemanticSearchResult;
import com.cartethyia.easyorange.ai.service.AiQaService;
import com.cartethyia.easyorange.ai.service.SemanticSearchService;
import com.cartethyia.easyorange.common.annotation.SkipRateLimit;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 客服问答端点 — 商品问答（LLM 生成）与语义搜索（embedding 召回）。
 */
@SkipRateLimit
@Tag(name = "AI 服务", description = "AI 客服问答：商品问答 / 语义搜索")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiQaController {

    private final SemanticSearchService semanticSearchService;
    private final AiQaService qaService;

    @GetMapping("/semantic-search")
    public Result<SemanticSearchResult> semanticSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(semanticSearchService.search(keyword, pageNum, pageSize));
    }

    @PostMapping("/qa")
    public Result<QaResponse> answerQuestion(@Valid @RequestBody QaRequest request) {
        return Result.success(qaService.answerQuestion(request));
    }
}
