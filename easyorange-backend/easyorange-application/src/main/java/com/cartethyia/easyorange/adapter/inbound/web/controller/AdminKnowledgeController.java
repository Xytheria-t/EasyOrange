package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.adapter.inbound.web.assembler.KnowledgeDocAssembler;
import com.cartethyia.easyorange.adapter.inbound.web.request.CreateKnowledgeDocRequest;
import com.cartethyia.easyorange.adapter.inbound.web.response.KnowledgeDocVO;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeRepository;
import com.cartethyia.easyorange.ai.service.KnowledgeIngestionService;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库文档管理（管理端）— 新增即摄入（分块 → embed → ES），列表 / 删除 / 补索引。
 * /api/admin/** 由 SecurityConfig 统一限 ADMIN 角色。
 */
@Tag(name = "平台运维", description = "RAG 知识库文档管理（摄入 / 列表 / 删除 / 补索引）")
@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
public class AdminKnowledgeController {

    private final KnowledgeIngestionService ingestionService;
    private final KnowledgeRepository repository;

    /** 新增文档并摄入：解析 → 分块 → embed → ES 索引（best-effort）。 */
    @PostMapping
    public Result<String> create(@Valid @RequestBody CreateKnowledgeDocRequest request) {
        String id = ingestionService.ingest(request.title(), request.content(), request.source());
        return Result.success(id);
    }

    @GetMapping
    public Result<PageResult<KnowledgeDocVO>> page(
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(KnowledgeDocAssembler.toVOPage(repository.page(pageNum, pageSize)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        ingestionService.delete(id);
        return Result.success();
    }

    /** 补索引：把上次摄入失败/ES 不可用的 PENDING 文档全部重试一遍。 */
    @PostMapping("/reindex")
    public Result<Integer> reindex() {
        return Result.success(ingestionService.reindexAllPending());
    }
}
