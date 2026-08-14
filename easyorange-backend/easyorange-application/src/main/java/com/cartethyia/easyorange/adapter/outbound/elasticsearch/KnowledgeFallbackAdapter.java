package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.cartethyia.easyorange.ai.knowledge.KnowledgeChunk;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeIndexPort;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 知识库检索降级适配器（ES 关闭时激活）— 标题/正文 LIKE 检索，仅保证可用不保证召回质量。
 * <p>
 * isAvailable=false 让摄入侧保持 PENDING（启动补索引重试），检索侧走 LIKE 兜底，
 * 与商品搜索「ES 关闭 → MySQL search_text」的降级策略一致。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class KnowledgeFallbackAdapter implements KnowledgeIndexPort {

    private final KnowledgeRepository repository;

    @Override
    public void ingestChunks(List<KnowledgeChunk> chunks) {
        log.debug("Knowledge index disabled, ingest skipped (docs stay PENDING)");
    }

    @Override
    public void removeDoc(String docId) {
        // 无索引可删
    }

    @Override
    public List<KnowledgeChunk> search(String query, List<Float> queryEmbedding, int topK) {
        return repository.searchByContent(query, topK).stream()
                .map(doc -> new KnowledgeChunk(doc.id(), 0, doc.title(), doc.content(), null))
                .toList();
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
