package com.cartethyia.easyorange.ai.application.retrieval;

import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import com.cartethyia.easyorange.ai.domain.enums.KnowledgeDocStatus;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeChunk;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeDocEntity;
import com.cartethyia.easyorange.ai.domain.port.KnowledgeIndexPort;
import com.cartethyia.easyorange.ai.domain.port.KnowledgeRepository;
import com.cartethyia.easyorange.common.result.PageResult;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RAG 文档摄入管线 — 解析 → 分块（chunk size + overlap）→ embed → ES 索引。
 * <p>
 * <b>要么完整要么 FAILED</b>：embed 或 ES 写入失败整篇标 FAILED（启动补索引与管理端 reindex 会重试
 * PENDING 与 FAILED），不再「缺向量块照写」—— 那会让 INDEXED 掩盖「语义召回静默缺失」。索引不可用
 * （ES 未启用）时文档保持 PENDING。任何失败都不阻塞文档落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIngestionAppService {

    /** 单块字符数 — 平台规则类短文档（标题级知识库）500 字足够，无需过细粒度。 */
    static final int CHUNK_SIZE = 500;
    /** 块间重叠字符数 — 避免切点恰好切断关键句子。 */
    static final int CHUNK_OVERLAP = 50;

    private final KnowledgeRepository repository;
    private final ObjectProvider<KnowledgeIndexPort> indexPortProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final AiModelSupport aiModelSupport;

    /** 摄入一篇文档：落库 → 分块 → 逐块 embed → 批量写 ES 索引 → 回填状态。 */
    public String ingest(String title, String content, String source) {
        String id = repository.save(
                new KnowledgeDocEntity(null, title, content, source, KnowledgeDocStatus.PENDING, 0, null));
        indexChunks(id, title, content);
        return id;
    }

    /** 重新摄入已存在的文档（启动补索引用）— 保持文档 ID 稳定（金标准集引用同一批 ID）。
     * FAILED 一并重试：它是「ES 可用但 embed/写失败」的产物，与 PENDING（当时索引不可用）同属未完成态。 */
    public void reindexPending(String id) {
        var doc = repository.findById(id).orElse(null);
        if (doc == null || (doc.status() != KnowledgeDocStatus.PENDING && doc.status() != KnowledgeDocStatus.FAILED)) {
            return;
        }
        indexChunks(id, doc.title(), doc.content());
    }

    /** 全量补索引：把所有未完成（PENDING/FAILED）文档重试一遍（管理端 reindex 入口），返回重试的文档数。 */
    public int reindexIncomplete() {
        int page = 1;
        int total = 0;
        while (true) {
            var docs = repository.page(page, 50);
            for (var doc : docs.records()) {
                if (doc.status() != KnowledgeDocStatus.PENDING && doc.status() != KnowledgeDocStatus.FAILED) {
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
            if (embeddingModel == null) {
                throw new IllegalStateException("embedding model unconfigured");
            }
            // 整批一次供应商调用（逐块串行是 N 倍往返）；失败整篇 FAILED 待补索引
            List<List<Float>> vectors =
                    aiModelSupport.embedBatch(embeddingModel, AiCallScope.KNOWLEDGE, embedTexts(title, chunks));
            List<KnowledgeChunk> docs = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                docs.add(new KnowledgeChunk(id, i, title, chunks.get(i), vectors.get(i)));
            }
            // 重摄前清旧块：内容改版块数变少时，确定性 _id 覆盖不到旧高序号块，会留孤儿
            port.removeDoc(id);
            port.ingestChunks(docs);
            repository.updateStatus(id, KnowledgeDocStatus.INDEXED, chunks.size());
            log.info("Knowledge doc {} ingested: {} chunks", id, chunks.size());
        } catch (Exception e) {
            log.error("Knowledge doc {} ingestion failed, marked FAILED (bootstrap retries it)", id, e);
            repository.updateStatus(id, KnowledgeDocStatus.FAILED, chunks.size());
        }
    }

    /** 删除文档：逻辑删除 + 同步移除 ES 分块。 */
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

    /** 文档列表（管理端）：分页读仓储，不经检索链路 —— 看的是「库里有什么」，不是「搜得到什么」。 */
    @Transactional(readOnly = true)
    public PageResult<KnowledgeDocEntity> pageDocs(int pageNum, int pageSize) {
        return repository.page(pageNum, pageSize);
    }

    /**
     * 分块算法：固定 chunk size + overlap，切点优先落在换行处；纯静态便于单测覆盖。
     * <p>
     * 前进量的下界是 {@code start + 1} 而不是 {@code start}：切点回退到换行处时
     * {@code end - CHUNK_OVERLAP} 可能不大于 {@code start}（超长单行文档的换行落在窗口之外），
     * 少了这个钳位 while 会在同一位置空转。
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

    private static List<String> embedTexts(String title, List<String> chunks) {
        return chunks.stream().map(chunk -> title + "\n" + chunk).toList();
    }
}
