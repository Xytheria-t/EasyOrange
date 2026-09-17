package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.cartethyia.easyorange.ai.domain.model.KnowledgeChunk;
import com.cartethyia.easyorange.ai.domain.port.KnowledgeIndexPort;
import com.cartethyia.easyorange.ai.domain.port.KnowledgeRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
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
 * <p>
 * <b>降级必须可观测</b>：召回质量与 ES 路径差距悬殊（同一金标准集实测 hit@5 10% vs 100%），
 * 静默降级会让「RAG 不可用」看起来像「RAG 正常但答得差」。因此这里启动打一条 WARN 说明后果，
 * 并按调用次数累计 {@code easyorange.ai.rag.degraded}，供告警与大盘区分两条路径。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class KnowledgeFallbackAdapter implements KnowledgeIndexPort {

    private final KnowledgeRepository repository;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    void warnDegradedPath() {
        log.warn("RAG 检索运行在降级路径（ES 未启用）：召回退化为标题/正文 LIKE，"
                + "同一金标准集实测 hit@5 10%，对比 ES 路径 100%。"
                + "生产环境应设 easyorange.search.elasticsearch.enabled=true（ProdSearchGuard 会强制校验）。"
                + "降级调用计数见指标 easyorange.ai.rag.degraded");
    }

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
        meterRegistry.counter("easyorange.ai.rag.degraded", "op", "search").increment();
        return repository.searchByContent(query, topK).stream()
                .map(doc -> new KnowledgeChunk(doc.id(), 0, doc.title(), doc.content(), null))
                .toList();
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
