package com.cartethyia.easyorange.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.adapter.inbound.web.response.KnowledgeDocVO;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeDocEntity;
import com.cartethyia.easyorange.common.result.PageResult;

/**
 * 知识库文档视图组装 — 实体 → VO（剔除正文大字段），分页整体映射。
 */
public final class KnowledgeDocAssembler {

    private KnowledgeDocAssembler() {}

    public static KnowledgeDocVO toVO(KnowledgeDocEntity entity) {
        return new KnowledgeDocVO(
                entity.id(),
                entity.title(),
                entity.source(),
                entity.status(),
                entity.chunkCount(),
                entity.createTime());
    }

    public static PageResult<KnowledgeDocVO> toVOPage(PageResult<KnowledgeDocEntity> page) {
        return new PageResult<>(
                page.records().stream().map(KnowledgeDocAssembler::toVO).toList(),
                page.total(),
                page.current(),
                page.size(),
                page.pages());
    }
}
