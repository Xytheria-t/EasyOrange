package com.cartethyia.easyorange.ai.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.ai.adapter.inbound.web.dto.response.KnowledgeHitVO;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import java.util.List;

/**
 * 知识库命中视图组装 — 检索侧 domain 模型 → 对外 VO，列表整体映射。
 */
public final class KnowledgeHitAssembler {

    private KnowledgeHitAssembler() {}

    public static KnowledgeHitVO toVO(KnowledgeHit hit) {
        return new KnowledgeHitVO(hit.docId(), hit.title(), hit.content(), hit.score());
    }

    public static List<KnowledgeHitVO> toVOList(List<KnowledgeHit> hits) {
        return hits.stream().map(KnowledgeHitAssembler::toVO).toList();
    }
}
