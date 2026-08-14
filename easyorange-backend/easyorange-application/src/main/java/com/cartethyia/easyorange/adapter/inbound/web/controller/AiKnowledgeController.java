package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.ai.knowledge.KnowledgeHit;
import com.cartethyia.easyorange.ai.service.KnowledgeRetrievalService;
import com.cartethyia.easyorange.common.annotation.SkipRateLimit;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库检索端点（RAG 检索侧演示）— 与聊天引用溯源共用 KnowledgeRetrievalService。
 */
@SkipRateLimit
@Tag(name = "AI 知识库", description = "RAG 知识库混合召回（kNN + BM25 + Cosine 重排）")
@RestController
@RequestMapping("/api/ai/knowledge")
@RequiredArgsConstructor
public class AiKnowledgeController {

    private final KnowledgeRetrievalService retrievalService;

    @GetMapping("/search")
    public Result<List<KnowledgeHit>> search(@RequestParam String keyword, @RequestParam(defaultValue = "5") int topK) {
        return Result.success(retrievalService.search(keyword, topK));
    }
}
