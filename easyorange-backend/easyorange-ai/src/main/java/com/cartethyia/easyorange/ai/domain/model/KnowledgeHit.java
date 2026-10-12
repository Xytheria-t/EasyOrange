package com.cartethyia.easyorange.ai.domain.model;

import java.util.List;

/** 知识库检索命中 — 引用溯源的最小单元（回答末尾用 [来源:标题] 标注）。score 由 {@code KnowledgeChunkHit} 透传，是索引侧 RRF 排名融合分，LIKE 降级路径恒为 0。 */
public record KnowledgeHit(String docId, String title, String content, double score) {

    /**
     * 命中清单的文本渲染 — 生成 prompt 两处装配共用（{@code ChatPromptAssembler} / {@code ListingPromptAssembler}）：
     * 同一份命中渲染成同一种形状，空集的缺省标记只有一处定义。
     */
    public static String format(List<KnowledgeHit> hits) {
        if (hits.isEmpty()) {
            return "(无检索结果)";
        }
        var sb = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            KnowledgeHit hit = hits.get(i);
            sb.append("[%d] (%s)\n%s\n".formatted(i + 1, hit.title(), hit.content()));
        }
        return sb.toString();
    }
}
