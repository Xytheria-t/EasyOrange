package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeChunk;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeIndexPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.Queries;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 知识库向量索引（ES 适配器）— 实现 {@link KnowledgeIndexPort}。
 * <p>
 * 写入 best-effort（try-catch 只告警，索引失败不阻塞主链路，与商品索引同一语义）；
 * 检索为 kNN（embedding）+ BM25（title^2/content）混合召回，重排由业务侧 Cosine 收口。
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
public class KnowledgeElasticsearchAdapter implements KnowledgeIndexPort {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;

    @Override
    public void ingestChunks(List<KnowledgeChunk> chunks) {
        try {
            for (KnowledgeChunk chunk : chunks) {
                elasticsearchOperations.save(toDocument(chunk));
            }
        } catch (Exception e) {
            log.warn(
                    "Knowledge chunk ingest failed (best-effort), docId={}",
                    chunks.isEmpty() ? "?" : chunks.getFirst().docId(),
                    e);
        }
    }

    @Override
    public void removeDoc(String docId) {
        try {
            var deleteQuery = DeleteQuery.builder(new StringQuery("{\"term\":{\"docId\":\"" + docId + "\"}}"))
                    .build();
            elasticsearchOperations.delete(deleteQuery, KnowledgeChunkDocument.class);
        } catch (Exception e) {
            log.warn("Remove knowledge chunks failed (best-effort), docId={}", docId, e);
        }
    }

    @Override
    public List<KnowledgeChunk> search(String query, List<Float> queryEmbedding, int topK) {
        boolean useKnn = queryEmbedding != null && !queryEmbedding.isEmpty();

        var queryBuilder = NativeQuery.builder().withPageable(PageRequest.of(0, topK));
        if (useKnn) {
            queryBuilder.withKnnSearches(knn ->
                    knn.field("embedding").queryVector(queryEmbedding).k(topK).numCandidates(100));
        }
        queryBuilder.withQuery(Queries.wrapperQueryAsQuery(buildBm25Query(query).toString()));
        queryBuilder.withSort(List.of(SortOptions.of(so -> so.score(s -> s.order(SortOrder.Desc)))));

        SearchHits<KnowledgeChunkDocument> hits;
        try {
            hits = elasticsearchOperations.search(queryBuilder.build(), KnowledgeChunkDocument.class);
        } catch (Exception e) {
            log.warn("Knowledge search failed, return empty", e);
            return List.of();
        }
        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toChunk)
                .toList();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private JsonNode buildBm25Query(String query) {
        ObjectNode bool = objectMapper.createObjectNode();
        var must = objectMapper.createArrayNode();
        if (query != null && !query.isBlank()) {
            var multiMatch = objectMapper.createObjectNode();
            multiMatch.put("query", query);
            multiMatch.put("type", "best_fields");
            multiMatch.put("fuzziness", "AUTO");
            var fields = multiMatch.putArray("fields");
            fields.add("title^2");
            fields.add("content");
            must.add(objectMapper.createObjectNode().set("multi_match", multiMatch));
        } else {
            must.add(objectMapper.createObjectNode().set("match_all", objectMapper.createObjectNode()));
        }
        bool.set("must", must);
        return objectMapper.createObjectNode().set("bool", bool);
    }

    private static KnowledgeChunkDocument toDocument(KnowledgeChunk chunk) {
        return KnowledgeChunkDocument.builder()
                .id(chunk.docId() + ":" + chunk.chunkIndex())
                .docId(chunk.docId())
                .chunkIndex(chunk.chunkIndex())
                .title(chunk.title())
                .content(chunk.content())
                .embedding(chunk.embedding())
                .build();
    }

    private KnowledgeChunk toChunk(KnowledgeChunkDocument doc) {
        return new KnowledgeChunk(
                doc.getDocId(), doc.getChunkIndex(), doc.getTitle(), doc.getContent(), doc.getEmbedding());
    }
}
