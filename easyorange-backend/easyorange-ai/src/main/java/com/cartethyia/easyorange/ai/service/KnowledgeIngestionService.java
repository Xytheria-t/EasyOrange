package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.enums.KnowledgeDocStatus;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeChunk;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeDocEntity;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeIndexPort;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * RAG 文档摄入管线 — 解析 → 分块（chunk size + overlap）→ embed → ES 索引。
 * <p>
 * <b>best-effort 写入</b>：分块是纯本地计算必然成功；embed 失败的分块向量为 null
 * （该块仍写入索引，仅缺失语义召回能力）；索引不可用（ES 未启用）时文档保持
 * PENDING，由启动补索引任务重试。任何失败都不阻塞文档落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIngestionService {

    /** 单块字符数 — 平台规则类短文档（标题级知识库）500 字足够，无需过细粒度。 */
    static final int CHUNK_SIZE = 500;
    /** 块间重叠字符数 — 避免切点恰好切断关键句子。 */
    static final int CHUNK_OVERLAP = 50;

    private final KnowledgeRepository repository;
    private final ObjectProvider<KnowledgeIndexPort> indexPortProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final AiModelSupport aiModelSupport;

    /**
     * 摄入一篇文档：落库 → 分块 → 逐块 embed → 批量写 ES 索引 → 回填状态。
     *
     * @return 文档 ID
     */
    public String ingest(String title, String content, String source) {
        String id = repository.save(
                new KnowledgeDocEntity(null, title, content, source, KnowledgeDocStatus.PENDING, 0, null));
        indexChunks(id, title, content);
        return id;
    }

    /**
     * 重新摄入已存在的文档（启动补索引用）— 保持文档 ID 稳定（金标准集引用同一批 ID）。
     */
    public void reindexPending(String id) {
        var doc = repository.findById(id).orElse(null);
        if (doc == null || doc.status() != KnowledgeDocStatus.PENDING) {
            return;
        }
        indexChunks(id, doc.title(), doc.content());
    }

    /**
     * 全量补索引：把所有 PENDING 文档重试一遍（管理端 /api/admin/knowledge/reindex 入口）。
     *
     * @return 重试的文档数
     */
    public int reindexAllPending() {
        int page = 1;
        int total = 0;
        while (true) {
            var docs = repository.page(page, 50);
            for (var doc : docs.records()) {
                if (doc.status() != KnowledgeDocStatus.PENDING) {
                    continue;
                }
                reindexPending(doc.id());
                total++;
            }
            if (docs.current() >= docs.pages() || docs.records().isEmpty()) {
                break;
            }
            page++;
        }
        return total;
    }

    private void indexChunks(String id, String title, String content) {
        var port = indexPortProvider.getIfAvailable();
        if (port == null || !port.isAvailable()) {
            log.info("Knowledge index unavailable, doc {} stays PENDING for retry", id);
            return;
        }

        List<String> chunks = chunkContent(content);
        try {
            var embeddingModel = embeddingModelProvider.getIfAvailable();
            List<KnowledgeChunk> docs = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                docs.add(new KnowledgeChunk(
                        id, i, title, chunks.get(i), bestEffortEmbed(embeddingModel, title + "\n" + chunks.get(i))));
            }
            port.ingestChunks(docs);
            repository.updateStatus(id, KnowledgeDocStatus.INDEXED, chunks.size());
            log.info("Knowledge doc {} ingested: {} chunks", id, chunks.size());
        } catch (Exception e) {
            log.error("Knowledge doc {} ingestion failed, marked FAILED", id, e);
            repository.updateStatus(id, KnowledgeDocStatus.FAILED, chunks.size());
        }
    }

    /**
     * 删除文档：逻辑删除 + 同步移除 ES 分块。
     */
    public void delete(String id) {
        repository.deleteById(id);
        var port = indexPortProvider.getIfAvailable();
        if (port != null && port.isAvailable()) {
            try {
                port.removeDoc(id);
            } catch (Exception e) {
                log.warn("Remove knowledge doc {} from index failed", id, e);
            }
        }
    }

    /**
     * 分块算法：固定 chunk size + overlap，切点优先落在换行处（避免切断句子）。
     * 纯静态便于单测覆盖。
     */
    static List<String> chunkContent(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        var chunks = new ArrayList<String>();
        int length = content.length();
        int start = 0;
        while (start < length) {
            int end = Math.min(start + CHUNK_SIZE, length);
            if (end < length) {
                int newline = content.lastIndexOf('\n', end);
                if (newline > start + CHUNK_SIZE / 2) {
                    end = newline;
                }
            }
            String chunk = content.substring(start, end).strip();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end >= length) {
                break;
            }
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return chunks;
    }

    private List<Float> bestEffortEmbed(EmbeddingModel model, String text) {
        if (model == null) {
            return null;
        }
        try {
            return aiModelSupport.embed(model, text);
        } catch (Exception e) {
            log.warn("Knowledge chunk embed failed, chunk falls back to text-only: {}", e.getMessage());
            return null;
        }
    }
}
