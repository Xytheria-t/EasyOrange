package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.stereotype.Component;

/**
 * 在启动时使用 JSON 配置文件编程式创建 ES 索引。
 * 仅在 easyorange.search.elasticsearch.enabled=true 时激活。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ElasticsearchIndexManager {

    private final ElasticsearchOperations elasticsearchOperations;

    @PostConstruct
    public void initIndices() {
        createProductIndex();
        createKnowledgeIndex();
    }

    /** 重建路径的入口（{@code ReindexService} 删索引后要按 mapping 复原，否则 ES 动态 auto-create 出错 mapping）。 */
    void createProductIndex() {
        createIndex(ProductDocument.class, "elasticsearch/product-mapping.json", "products", "ES index");
    }

    /** RAG 知识库分块索引（dense_vector 1024 与 text-embedding-v3 对齐，复用 IK 分词 settings）。 */
    void createKnowledgeIndex() {
        createIndex(
                KnowledgeChunkDocument.class,
                "elasticsearch/knowledge-mapping.json",
                "knowledge_docs",
                "ES knowledge index");
    }

    /**
     * 建索引的唯一路径：两个索引只在「文档类 / mapping 文件 / 索引名」上不同。
     * 已存在则跳过 —— 启动与重建都靠这一行保证幂等。
     */
    private void createIndex(Class<?> documentClass, String mappingFile, String indexName, String errorPrefix) {
        IndexOperations indexOps = elasticsearchOperations.indexOps(documentClass);
        if (indexOps.exists()) {
            log.info("ES index '{}' already exists, skipping creation", indexName);
            return;
        }
        try {
            // 两个索引共用同一份 IK 分词 settings，mapping 各自一份
            indexOps.create(Settings.parse(readJson("elasticsearch/product-settings.json")));
            indexOps.putMapping(Document.parse(readJson(mappingFile)));
            log.info("Created ES index '{}' with IK analyzer mapping", indexName);
        } catch (Exception e) {
            log.error("Failed to create ES index '{}'", indexName, e);
            throw BusinessException.of(ResultCode.INTERNAL_SERVER_ERROR, errorPrefix + " creation failed", e);
        }
    }

    private static String readJson(String classpath) throws IOException {
        return new String(new ClassPathResource(classpath).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
