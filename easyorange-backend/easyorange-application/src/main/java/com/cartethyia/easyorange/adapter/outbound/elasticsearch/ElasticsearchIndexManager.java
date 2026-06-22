package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
    }

    void createProductIndex() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);

        if (indexOps.exists()) {
            log.info("ES index 'products' already exists, skipping creation");
            return;
        }

        try {
            String settingsJson = readJson("elasticsearch/product-settings.json");
            String mappingJson = readJson("elasticsearch/product-mapping.json");

            indexOps.create(Settings.parse(settingsJson));
            indexOps.putMapping(Document.parse(mappingJson));

            log.info("Created ES index 'products' with IK analyzer mapping");
        } catch (Exception e) {
            log.error("Failed to create ES index 'products'", e);
            throw BusinessException.of(ResultCode.INTERNAL_SERVER_ERROR, "ES index creation failed", e);
        }
    }

    private static String readJson(String classpath) throws IOException {
        return new String(new ClassPathResource(classpath).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
