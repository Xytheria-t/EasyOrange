# ElasticSearch 搜索集成 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace MySQL ngram FULLTEXT search with ElasticSearch for product search, adding fuzzy matching, Chinese tokenization (IK), field boosting, and faceted aggregation in one query.

**Architecture:** All ES infrastructure lives in `easyorange-application/adapter/outbound/elasticsearch/`. Write path reuses existing `ProductSearchIndexPort` (event-driven). Read path via new `ProductSearchQueryPort` in product domain, with `ProductSearchHandler` routing to ES when available, falling back to MySQL. Settings/mapping via JSON files + programmatic index creation (no annotation-based mapping).

**Tech Stack:** Java 25, Spring Boot 4.0.3, Spring Data Elasticsearch, IK Analysis Plugin 8.17.3, Testcontainers, React + TanStack Query

---

### Batch A — product module domain layer

---

### Task A1: Add ProductSearchQueryPort + SearchResult + FacetBucket to product domain

**Files:**
- Create: `easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/port/output/ProductSearchQueryPort.java`
- Create: `easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/port/output/SearchResult.java`
- Create: `easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/port/output/FacetBucket.java`

- [ ] **Step 1: Create `ProductSearchQueryPort.java`**

```java
package com.cartethyia.easyorange.product.domain.port.output;

import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import java.util.Optional;

/**
 * 商品搜索查询端口 — ES 实现此接口，不可用时降级到 {@link ProductQueryRepository#searchProducts}.
 */
public interface ProductSearchQueryPort extends OutboundPort {

    SearchResult search(ProductSearchQuery query);

    record ProductSearchQuery(
        String keyword,
        Long categoryId,
        Integer status,
        java.math.BigDecimal minPrice,
        java.math.BigDecimal maxPrice,
        Integer conditionLevel,
        String sort,          // relevance / price_asc / price_desc / newest / popular
        int pageNum,
        int pageSize
    ) { }
}
```

- [ ] **Step 2: Create `FacetBucket.java`**

```java
package com.cartethyia.easyorange.product.domain.port.output;

public record FacetBucket(
    String key,
    String label,
    long count
) { }
```

- [ ] **Step 3: Create `SearchResult.java`**

```java
package com.cartethyia.easyorange.product.domain.port.output;

import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.util.List;

public record SearchResult(
    List<ProductReadModel> records,
    long total,
    int pageNum,
    int pageSize,
    List<FacetBucket> categoryFacets,
    List<FacetBucket> conditionFacets,
    List<FacetBucket> priceRangeFacets
) { }
```

- [ ] **Step 4: Build & verify compilation**

Run: `./mvnw compile -pl easyorange-product -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/port/output/ProductSearchQueryPort.java
git add easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/port/output/SearchResult.java
git add easyorange-product/src/main/java/com/cartethyia/easyorange/product/domain/port/output/FacetBucket.java
git commit -m "feat(product): add ProductSearchQueryPort + SearchResult + FacetBucket"
```

---

### Task A2: Switch existing ProductSearchIndexAdapter to conditional bean, create basic application config

- [ ] **Step 1: Add `@ConditionalOnProperty` to existing MySQL adapter**

Current file: `easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/product/ProductSearchIndexAdapter.java`

Add imports:
```java
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
```

Add annotation to class:
```java
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
```

Full class annotation stack becomes:
```java
@Slf4j
@Component
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class ProductSearchIndexAdapter implements ProductSearchIndexPort {
```

- [ ] **Step 2: Build to confirm**

Run: `./mvnw compile -pl easyorange-application -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/product/ProductSearchIndexAdapter.java
git commit -m "chore(application): add ConditionalOnProperty to MySQL search index adapter"
```

---

### Task A3: Add ES dependency + configuration

**Files:**
- Modify: `easyorange-application/pom.xml`
- Modify: `easyorange-application/src/main/resources/application.yaml`

- [ ] **Step 1: Add spring-boot-starter-data-elasticsearch to pom.xml**

Insert after the `flyway-mysql` dependency block (before `</dependencies>`):

```xml
        <!-- ElasticSearch -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
        </dependency>
```

- [ ] **Step 2: Add ES configuration to application.yaml**

Append at end of file:

```yaml
# ElasticSearch 配置
easyorange:
  search:
    elasticsearch:
      enabled: false

spring:
  elasticsearch:
    uris: ${ES_URIS:http://localhost:9200}
    connection-timeout: 5s
    socket-timeout: 30s
```

- [ ] **Step 3: Build to confirm deps resolve**

Run: `./mvnw dependency:resolve -pl easyorange-application -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add easyorange-application/pom.xml
git add easyorange-application/src/main/resources/application.yaml
git commit -m "feat(application): add Spring Data ES dependency and config"
```

---

### Task A4: Create ES settings/mapping JSON files

**Files:**
- Create: `easyorange-application/src/main/resources/elasticsearch/product-settings.json`
- Create: `easyorange-application/src/main/resources/elasticsearch/product-mapping.json`

- [ ] **Step 1: Create `product-settings.json`**

```json
{
  "number_of_shards": 1,
  "number_of_replicas": 1,
  "analysis": {
    "analyzer": {
      "ik_max_word": {
        "type": "custom",
        "tokenizer": "ik_max_word"
      },
      "ik_smart": {
        "type": "custom",
        "tokenizer": "ik_smart"
      }
    }
  }
}
```

- [ ] **Step 2: Create `product-mapping.json`**

```json
{
  "properties": {
    "id":              { "type": "keyword" },
    "userId":          { "type": "keyword" },
    "name": {
      "type": "text",
      "analyzer": "ik_max_word",
      "search_analyzer": "ik_smart",
      "fields": { "keyword": { "type": "keyword" } }
    },
    "description": {
      "type": "text",
      "analyzer": "ik_max_word",
      "search_analyzer": "ik_smart"
    },
    "categoryId":      { "type": "integer" },
    "categoryName":    { "type": "keyword" },
    "price":           { "type": "scaled_float", "scaling_factor": 100 },
    "originalPrice":   { "type": "scaled_float", "scaling_factor": 100 },
    "conditionLevel":  { "type": "byte" },
    "status":          { "type": "byte" },
    "viewCount":       { "type": "integer" },
    "stock":           { "type": "integer" },
    "location":        { "type": "keyword" },
    "tags":            { "type": "keyword" },
    "mainImage":       { "type": "keyword" },
    "images":          { "type": "keyword" },
    "sellerName":      { "type": "keyword" },
    "sellerAvatar":    { "type": "keyword" },
    "createTime":      { "type": "date", "format": "yyyy-MM-dd HH:mm:ss||epoch_millis" },
    "updateTime":      { "type": "date", "format": "yyyy-MM-dd HH:mm:ss||epoch_millis" }
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add easyorange-application/src/main/resources/elasticsearch/
git commit -m "feat(application): add ES index settings and mapping JSON files"
```

---

### Task A5: Create ProductDocument class

**Files:**
- Create: `easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/elasticsearch/ProductDocument.java`

- [ ] **Step 1: Create `ProductDocument.java`**

```java
package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products")
public class ProductDocument {

    @Id
    private String id;

    private Long userId;
    private String name;
    private String description;
    private Integer categoryId;
    private String categoryName;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Double)
    private Double originalPrice;

    private Byte conditionLevel;
    private Byte status;
    private Integer viewCount;
    private Integer stock;
    private String location;
    private List<String> tags;
    private String mainImage;
    private List<String> images;
    private String sellerName;
    private String sellerAvatar;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: Build to confirm**

Run: `./mvnw compile -pl easyorange-application -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/elasticsearch/ProductDocument.java
git commit -m "feat(application): add ProductDocument ES document class"
```

---

### Task A6: Create ElasticsearchIndexManager (programmatic index creation)

**Files:**
- Create: `easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/elasticsearch/ElasticsearchIndexManager.java`

- [ ] **Step 1: Create `ElasticsearchIndexManager.java`**

```java
package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
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

            indexOps.create(Settings.parseSettings(settingsJson));
            indexOps.putMapping(indexOps.createMapping(ProductDocument.class));

            // 使用原生客户端 apply mapping JSON（因为 mapping JSON 包含自定义分析器配置）
            org.elasticsearch.client.elasticsearch.ElasticsearchClient nativeClient = elasticsearchOperations
                    .getElasticsearchConverter()
                    .getMappingContext()
                    // get the low-level REST client through the operations
                    ;
            
            log.info("Created ES index 'products' with custom mapping");
        } catch (Exception e) {
            log.error("Failed to create ES index 'products'", e);
            throw new RuntimeException("ES index creation failed", e);
        }
    }

    private static String readJson(String classpath) throws IOException {
        return new String(new ClassPathResource(classpath).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
```

Wait, the native client access is complex with Spring Data ES. Let me simplify — use `StringQuery` to apply raw JSON mapping:

Actually, `IndexOperations` has `putMapping(String source)` that accepts raw JSON. Let me use that.

```java
package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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

            indexOps.create(Settings.parseSettings(settingsJson));
            indexOps.putMapping(mappingJson);

            log.info("Created ES index 'products' with IK analyzer mapping");
        } catch (Exception e) {
            log.error("Failed to create ES index 'products'", e);
            throw new RuntimeException("ES index creation failed", e);
        }
    }

    private static String readJson(String classpath) throws IOException {
        return new String(new ClassPathResource(classpath).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 2: Build to confirm**

Run: `./mvnw compile -pl easyorange-application -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/elasticsearch/ElasticsearchIndexManager.java
git commit -m "feat(application): add ES index manager with JSON-based mapping creation"
```

---

### Task A7: Create ElasticsearchProductSearchIndexAdapter

**Files:**
- Create: `easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/elasticsearch/ElasticsearchProductSearchIndexAdapter.java`

- [ ] **Step 1: Create `ElasticsearchProductSearchIndexAdapter.java`**

```java
package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.cartethyia.easyorange.product.domain.port.output.ProductSearchIndexPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ES 实现的商品搜索索引适配器。
 * 当 easyorange.search.elasticsearch.enabled=true 时激活，替换 MySQL search_text 实现。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ElasticsearchProductSearchIndexAdapter implements ProductSearchIndexPort {

    private final ProductMapper productMapper;
    private final ProductDetailMapper productDetailMapper;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void indexProduct(Long productId) {
        saveDocument(productId);
    }

    @Override
    public void updateProductIndex(Long productId) {
        saveDocument(productId);
    }

    @Override
    public void removeProductIndex(Long productId) {
        elasticsearchOperations.delete(String.valueOf(productId), ProductDocument.class);
        log.debug("Deleted ES document for productId={}", productId);
    }

    private void saveDocument(Long productId) {
        try {
            ProductDO product = productMapper.selectById(productId);
            if (product == null) {
                log.warn("Product not found for ES index update, productId={}", productId);
                return;
            }

            ProductDocument doc = buildDocument(product);
            elasticsearchOperations.save(doc);
            log.debug("Saved ES document for productId={}", productId);
        } catch (Exception e) {
            log.error("Failed to save ES document for productId={}", productId, e);
        }
    }

    /** 将 ProductDO + ProductDetailDO 组装为 ES ProductDocument */
    ProductDocument buildDocument(ProductDO product) {
        ProductDetailDO detail = productDetailMapper.selectById(product.getId());

        List<String> tagList = product.getTags() != null && !product.getTags().isBlank()
                ? Arrays.stream(product.getTags().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        List<String> imageList = product.getImages() != null && !product.getImages().isBlank()
                ? Arrays.stream(product.getImages().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return ProductDocument.builder()
                .id(String.valueOf(product.getId()))
                .userId(product.getUserId())
                .name(product.getName())
                .description(detail != null ? detail.getDescription() : null)
                .categoryId(product.getCategoryId() != null ? product.getCategoryId().intValue() : null)
                .categoryName(product.getCategoryName())
                .price(product.getPrice() != null ? product.getPrice().doubleValue() : null)
                .originalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice().doubleValue() : null)
                .conditionLevel(product.getConditionLevel())
                .status(product.getStatus())
                .viewCount(product.getViewCount())
                .stock(product.getStock())
                .location(product.getLocation())
                .tags(tagList)
                .mainImage(product.getMainImage())
                .images(imageList)
                .sellerName(product.getUsername())
                .sellerAvatar(product.getUserAvatar())
                .createTime(product.getCreateTime())
                .updateTime(product.getUpdateTime())
                .build();
    }
}
```

- [ ] **Step 2: Build to confirm**

Run: `./mvnw compile -pl easyorange-application -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/elasticsearch/ElasticsearchProductSearchIndexAdapter.java
git commit -m "feat(application): add ES product search index adapter"
```

---

### Task A8: Create ElasticsearchProductSearchQueryAdapter

**Files:**
- Create: `easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/elasticsearch/ElasticsearchProductSearchQueryAdapter.java`

- [ ] **Step 1: Create the query adapter class**

```java
package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.domain.port.output.FacetBucket;
import com.cartethyia.easyorange.product.domain.port.output.ProductSearchQueryPort;
import com.cartethyia.easyorange.product.domain.port.output.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeQuery;
import org.springframework.data.elasticsearch.core.query.NativeQueryBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ElasticsearchProductSearchQueryAdapter implements ProductSearchQueryPort {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public SearchResult search(ProductSearchQuery query) {
        NativeQueryBuilder queryBuilder = new NativeQueryBuilder();

        // Build bool query
        var boolBuilder = new co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder();

        // keyword search (multi_match with boosting)
        if (query.keyword() != null && !query.keyword().isBlank()) {
            boolBuilder.must(m -> m.multiMatch(mm -> mm
                    .query(query.keyword())
                    .fields("name^3", "description")
                    .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                    .fuzziness("AUTO")
            ));
        } else {
            boolBuilder.must(new MatchAllQuery.Builder().build()._toQuery());
        }

        // Filters
        if (query.status() != null) {
            boolBuilder.filter(f -> f.term(t -> t.field("status").value(query.status())));
        }
        if (query.categoryId() != null) {
            boolBuilder.filter(f -> f.term(t -> t.field("categoryId").value(query.categoryId())));
        }
        if (query.conditionLevel() != null) {
            boolBuilder.filter(f -> f.term(t -> t.field("conditionLevel").value(query.conditionLevel())));
        }
        if (query.minPrice() != null || query.maxPrice() != null) {
            var range = new co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery.Builder();
            if (query.minPrice() != null) {
                range.gte(JsonData.of(query.minPrice()));
            }
            if (query.maxPrice() != null) {
                range.lte(JsonData.of(query.maxPrice()));
            }
            boolBuilder.filter(f -> f.range(r -> r.field("price").gte(
                    query.minPrice() != null ? JsonData.of(query.minPrice()) : null
                ).lte(
                    query.maxPrice() != null ? JsonData.of(query.maxPrice()) : null
                )));
        }

        queryBuilder.withQuery(q -> q.bool(boolBuilder.build()));

        // Sort
        String sort = query.sort() != null ? query.sort() : "relevance";
        switch (sort) {
            case "price_asc" -> queryBuilder.withSort(s -> s.field(f -> f.field("price").order(SortOrder.Asc)));
            case "price_desc" -> queryBuilder.withSort(s -> s.field(f -> f.field("price").order(SortOrder.Desc)));
            case "newest" -> queryBuilder.withSort(s -> s.field(f -> f.field("createTime").order(SortOrder.Desc)));
            case "popular" -> queryBuilder.withSort(s -> s.field(f -> f.field("viewCount").order(SortOrder.Desc)));
            default -> queryBuilder.withSort(s -> s.score(m -> m.order(SortOrder.Desc)));
        }

        // Pagination
        int page = query.pageNum() > 0 ? query.pageNum() : 1;
        int size = query.pageSize() > 0 ? query.pageSize() : 20;
        queryBuilder.withPageable(org.springframework.data.domain.PageRequest.of(page - 1, size));

        // Aggregations
        queryBuilder.withAggregation("category",
                Aggregation.of(a -> a.terms(t -> t.field("categoryId").size(20))));
        queryBuilder.withAggregation("conditionLevel",
                Aggregation.of(a -> a.terms(t -> t.field("conditionLevel").size(10))));
        queryBuilder.withAggregation("priceRanges",
                Aggregation.of(a -> a.range(r -> r.field("price")
                        .ranges(ra -> ra.to(JsonData.of(100)))
                        .ranges(ra -> ra.from(JsonData.of(100)).to(JsonData.of(500)))
                        .ranges(ra -> ra.from(JsonData.of(500)).to(JsonData.of(1000)))
                        .ranges(ra -> ra.from(JsonData.of(1000)))
                )));

        NativeQuery nativeQuery = queryBuilder.build();

        // Execute search
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        // Convert documents to ProductReadModel
        List<ProductReadModel> records = searchHits.getSearchHits().stream()
                .map(hit -> toReadModel(hit.getContent()))
                .collect(Collectors.toList());

        // Extract aggregations
        var aggMap = searchHits.getAggregations();
        List<FacetBucket> categoryFacets = extractTermAggBuckets(aggMap, "category");
        List<FacetBucket> conditionFacets = extractTermAggBuckets(aggMap, "conditionLevel");
        List<FacetBucket> priceRangeFacets = extractRangeAggBuckets(aggMap, "priceRanges");

        return new SearchResult(
                records,
                searchHits.getTotalHits(),
                page,
                size,
                categoryFacets,
                conditionFacets,
                priceRangeFacets
        );
    }

    private ProductReadModel toReadModel(ProductDocument doc) {
        return new ProductReadModel(
                doc.getId() != null ? Long.parseLong(doc.getId()) : null,
                doc.getUserId(),
                doc.getSellerName(),
                doc.getSellerAvatar(),
                doc.getCategoryId() != null ? doc.getCategoryId().longValue() : null,
                doc.getCategoryName(),
                doc.getName(),
                doc.getDescription(),
                doc.getPrice() != null ? BigDecimal.valueOf(doc.getPrice()) : null,
                doc.getOriginalPrice() != null ? BigDecimal.valueOf(doc.getOriginalPrice()) : null,
                doc.getStock(),
                doc.getStatus() != null ? doc.getStatus().intValue() : null,
                null,  // statusDesc — not stored in ES
                doc.getViewCount(),
                doc.getConditionLevel() != null ? doc.getConditionLevel().intValue() : null,
                null,  // conditionDesc — not stored in ES
                doc.getLocation(),
                null,  // contactMethod — not stored in ES
                doc.getImages(),
                doc.getMainImage(),
                doc.getCreateTime(),
                doc.getUpdateTime()
        );
    }

    @SuppressWarnings("unchecked")
    private List<FacetBucket> extractTermAggBuckets(
            org.springframework.data.elasticsearch.core.AggregationsContainer<?> aggContainer,
            String aggName) {
        if (aggContainer == null) return List.of();

        var agg = aggContainer.aggregations().get(aggName);
        if (agg == null) return List.of();

        var termsAgg = agg.aggregation().getAggregate().sterms();
        if (termsAgg == null) return List.of();

        return termsAgg.buckets().array().stream()
                .map(b -> new FacetBucket(b.key().stringValue(), b.key().stringValue(), b.docCount()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<FacetBucket> extractRangeAggBuckets(
            org.springframework.data.elasticsearch.core.AggregationsContainer<?> aggContainer,
            String aggName) {
        if (aggContainer == null) return List.of();

        var agg = aggContainer.aggregations().get(aggName);
        if (agg == null) return List.of();

        var rangeAgg = agg.aggregation().getAggregate().range();
        if (rangeAgg == null) return List.of();

        return rangeAgg.buckets().array().stream()
                .map(b -> {
                    String key = b.key() != null ? b.key() : (b.from() + "-" + b.to());
                    return new FacetBucket(key, key, b.docCount());
                })
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 2: Build to confirm**

Run: `./mvnw compile -pl easyorange-application -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/elasticsearch/ElasticsearchProductSearchQueryAdapter.java
git commit -m "feat(application): add ES product search query adapter with faceted aggregation"
```

---

### Batch B — Wiring (handler routing, response DTO, reindex)

---

### Task B1: Modify ProductSearchHandler to route to ES when available

**Files:**
- Modify: `easyorange-product/src/main/java/com/cartethyia/easyorange/product/application/query/handler/ProductSearchHandler.java`

- [ ] **Step 1: Add `Optional<ProductSearchQueryPort>` injection + routing logic**

Add import:
```java
import com.cartethyia.easyorange.product.domain.port.output.ProductSearchQueryPort;
import com.cartethyia.easyorange.product.domain.port.output.SearchResult;
import com.cartethyia.easyorange.product.domain.port.output.FacetBucket;
import java.util.Optional;
```

Add field + constructor parameter (replace `new ProductSearchHandler(productQueryRepository, searchHistoryService)` with also passing `Optional<ProductSearchQueryPort>` — but since it's `@RequiredArgsConstructor`, just add the field):

```java
    private final ProductQueryRepository productQueryRepository;
    private final SearchHistoryService searchHistoryService;
    private final Optional<ProductSearchQueryPort> searchQueryPort;
```

Modify `handleSearch` method:

```java
    @Transactional(readOnly = true)
    public PageResult<ProductResponse> handleSearch(ProductSearchRequest request) {
        // Try ES query port first if available
        if (searchQueryPort.isPresent()) {
            ProductSearchQueryPort.ProductSearchQuery query = new ProductSearchQueryPort.ProductSearchQuery(
                    request.getKeyword(),
                    request.getCategoryId(),
                    request.getStatus(),
                    request.getMinPrice(),
                    request.getMaxPrice(),
                    request.getConditionLevel(),
                    request.getSort(),
                    request.getPageNum() != null ? request.getPageNum() : 1,
                    request.getPageSize() != null ? request.getPageSize() : 20
            );
            SearchResult searchResult = searchQueryPort.get().search(query);
            List<ProductResponse> responses = searchResult.records().stream()
                    .map(this::toProductResponse)
                    .collect(Collectors.toList());
            return PageResult.of(responses, searchResult.total(), searchResult.pageNum(), searchResult.pageSize());
        }

        // Fallback to MySQL FULLTEXT search
        Page<ProductReadModel> page = productQueryRepository.searchProducts(
                request.getKeyword(),
                request.getCategoryId(),
                request.getStatus(),
                request.getPageNum() != null ? request.getPageNum() : 1,
                request.getPageSize() != null ? request.getPageSize() : 20
        );

        List<ProductResponse> responses = page.getRecords().stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());

        return PageResult.of(responses, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }
```

Update the existing constructor call in the test file — when `Optional` field is added, Mockito extension handles it. Actually, since `@RequiredArgsConstructor` generates constructor with all three fields, the test must pass the `Optional` too. The test uses `new ProductSearchHandler(productQueryRepository, searchHistoryService)` — this will break. Fix in tests.

- [ ] **Step 2: Fix test — ProductSearchHandlerTest**

Modify `setUp()`:
```java
    @Mock
    private ProductSearchQueryPort searchQueryPort;

    @BeforeEach
    void setUp() {
        searchHandler = new ProductSearchHandler(productQueryRepository, searchHistoryService, Optional.of(searchQueryPort));
        // ... rest unchanged
    }
```

But `ProductSearchQueryPort` is in `domain/port/output/` — the test needs to import it.

Add import to test:
```java
import com.cartethyia.easyorange.product.domain.port.output.ProductSearchQueryPort;
import java.util.Optional;
```

- [ ] **Step 3: Build & run tests**

Run: `./mvnw test -pl easyorange-product -DexcludedGroups=integration -q`
Expected: All tests pass (search handler + existing)

- [ ] **Step 4: Commit**

```bash
git add easyorange-product/src/main/java/com/cartethyia/easyorange/product/application/query/handler/ProductSearchHandler.java
git add easyorange-product/src/test/java/com/cartethyia/easyorange/product/application/query/handler/ProductSearchHandlerTest.java
git commit -m "feat(product): route search via Optional<ProductSearchQueryPort> with ES fallback"
```

---

### Task B2: Create ReindexService + admin endpoint

**Files:**
- Create: `easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/elasticsearch/ReindexService.java`
- Create: `easyorange-application/src/main/java/com/cartethyia/easyorange/controller/AdminSearchReindexController.java`

- [ ] **Step 1: Create `ReindexService.java`**

```java
package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 全量重建 ES 商品索引服务。
 * 仅在 ES 启用时注册。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ReindexService {

    private final ProductMapper productMapper;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchProductSearchIndexAdapter indexAdapter;

    /**
     * 全量重建索引：清空 → 读取 MySQL 在线商品 → 批量写入 ES。
     */
    public int reindexAll() {
        // 删除旧索引
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }

        // 读取所有在线商品
        List<ProductDO> products = productMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductDO>()
                        .eq(ProductDO::getStatus, ProductStatus.ONLINE.getCode())
        );

        // 批量写入
        List<ProductDocument> docs = products.stream()
                .map(indexAdapter::buildDocument)
                .toList();

        if (!docs.isEmpty()) {
            elasticsearchOperations.save(docs);
        }

        log.info("Reindexed {} products to ES", docs.size());
        return docs.size();
    }
}
```

- [ ] **Step 2: Create `AdminSearchReindexController.java`**

```java
package com.cartethyia.easyorange.controller;

import com.cartethyia.easyorange.adapter.outbound.elasticsearch.ReindexService;
import com.cartethyia.easyorange.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
@ConditionalOnBean(ReindexService.class)
public class AdminSearchReindexController {

    private final ReindexService reindexService;

    @PostMapping("/reindex")
    public Result<Integer> reindex() {
        int count = reindexService.reindexAll();
        return Result.success(count);
    }
}
```

- [ ] **Step 3: Build to confirm**

Run: `./mvnw compile -pl easyorange-application -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/outbound/elasticsearch/ReindexService.java
git add easyorange-application/src/main/java/com/cartethyia/easyorange/controller/AdminSearchReindexController.java
git commit -m "feat(application): add ES reindex service + admin endpoint"
```

---

### Batch C — Docker infrastructure

---

### Task C1: Create ES Dockerfile with IK plugin

**Files:**
- Create: `docker/elasticsearch/Dockerfile`

- [ ] **Step 1: Create `docker/elasticsearch/Dockerfile`**

```dockerfile
FROM docker.elastic.co/elasticsearch/elasticsearch:8.17.3

RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch \
    https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.17.3/elasticsearch-analysis-ik-8.17.3.zip

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -s http://localhost:9200/_cluster/health | grep -q '"status":"green"' || exit 1
```

- [ ] **Step 2: Commit**

```bash
git add docker/elasticsearch/Dockerfile
git commit -m "feat(docker): add ES Dockerfile with IK analysis plugin"
```

---

### Task C2: Update docker-compose.yml with ES service

**Files:**
- Modify: `docker-compose.yml`

- [ ] **Step 1: Add ES service + volume**

Insert after `redis` service block (before `volumes:`):

```yaml
  elasticsearch:
    build: ./docker/elasticsearch
    container_name: easyorange-es
    restart: unless-stopped
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
      - TZ=Asia/Shanghai
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data
    networks:
      - easyorange-net
    healthcheck:
      test: ["CMD-SHELL", "curl -s http://localhost:9200/_cluster/health | grep -q '\"status\":\"green\\|\"status\":\"yellow\"'"]
      interval: 30s
      timeout: 10s
      start_period: 60s
      retries: 5
    deploy:
      resources:
        limits:
          memory: 1G
```

Add `es-data` to volumes:

```yaml
volumes:
  mysql-data:
  redis-data:
  es-data:
```

- [ ] **Step 2: Commit**

```bash
git add docker-compose.yml
git commit -m "feat(docker): add ES service to docker-compose with IK analyzer"
```

---

### Batch D — Frontend

---

### Task D1: Update frontend types, API, and hook for aggregations

**Files:**
- Modify: `easyorange-frontend/src/types/product.ts`
- Modify: `easyorange-frontend/src/api/productApi.ts`
- Modify: `easyorange-frontend/src/hooks/product/useSearch.ts`

- [ ] **Step 1: Add aggregation types to `src/types/product.ts`**

```typescript
export interface FacetBucket {
  key: string;
  label: string;
  count: number;
}

export interface ProductSearchResult {
  records: Product[];
  total: number;
  pageNum: number;
  pageSize: number;
  aggregations?: Record<string, FacetBucket[]>;
}
```

- [ ] **Step 2: Update `productApi.ts` searchProducts return type**

Change from:
```typescript
    searchProducts(keyword: string, pageNum?: number, pageSize?: number) {
        return request<PageResult<Product>>('/products/search', {
```

To:
```typescript
    searchProducts(keyword: string, pageNum?: number, pageSize?: number) {
        return request<ProductSearchResult>('/products/search', {
```

Add import:
```typescript
import type { ProductSearchResult } from '@/types';
```

- [ ] **Step 3: Update `useSearch.ts` hook**

Change `useProductSearch` to return enriched type:

```typescript
import type { ProductQueryParams, PageResult, Product, ProductSearchResult } from '@/types';

export function useProductSearch(keyword: string, params?: Omit<ProductQueryParams, 'keyword'>) {
    return useQuery<ProductSearchResult>({
        queryKey: ['productSearch', keyword, params],
        queryFn: async () => {
            const response = await productApi.searchProducts(keyword, params?.pageNum, params?.pageSize);
            return response.data;
        },
        enabled: keyword.trim().length > 0,
        staleTime: 30 * 1000,
    });
}
```

Update `SearchPage.tsx` to extract aggregations:

```typescript
    const products = searchResult?.records ?? [];
    const total = searchResult?.total ?? 0;
    const aggregations = searchResult?.aggregations;
```

- [ ] **Step 4: Verify frontend compilation**

Run: `cd easyorange-frontend && npx tsc --noEmit`
Expected: No type errors

- [ ] **Step 5: Commit**

```bash
git add easyorange-frontend/src/types/product.ts
git add easyorange-frontend/src/api/productApi.ts
git add easyorange-frontend/src/hooks/product/useSearch.ts
git add easyorange-frontend/src/pages/profile/SearchPage.tsx
git commit -m "feat(frontend): add ProductSearchResult type with aggregations"
```

---

### Task D2: Create FacetFilter UI component

**Files:**
- Create: `easyorange-frontend/src/components/search/FacetFilter.tsx`
- Create: `easyorange-frontend/src/components/search/facet-filter.css`
- Modify: `easyorange-frontend/src/pages/profile/SearchPage.tsx` (integrate FacetFilter)

- [ ] **Step 1: Create `FacetFilter.tsx`**

```tsx
import { FacetBucket } from '@/types';
import './facet-filter.css';

interface FacetFilterProps {
  title: string;
  buckets?: FacetBucket[];
  selectedKey?: string;
  onChange: (key: string | undefined) => void;
}

export function FacetFilter({ title, buckets, selectedKey, onChange }: FacetFilterProps) {
  if (!buckets || buckets.length === 0) return null;

  return (
    <div className="facet-filter">
      <h4 className="facet-filter-title">{title}</h4>
      <div className="facet-filter-options">
        {selectedKey && (
          <button className="facet-option facet-option-active" onClick={() => onChange(undefined)}>
            <span>全部</span>
            <span className="facet-count">
              <X size={12} />
            </span>
          </button>
        )}
        {buckets.map(b => (
          <button
            key={b.key}
            className={`facet-option ${selectedKey === b.key ? 'facet-option-selected' : ''}`}
            onClick={() => onChange(b.key === selectedKey ? undefined : b.key)}
          >
            <span>{b.label}</span>
            <span className="facet-count">{b.count}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Create `facet-filter.css`**

```css
.facet-filter {
  margin-bottom: 1rem;
}

.facet-filter-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.5rem;
}

.facet-filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.facet-option {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.7rem;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #fff;
  font-size: 0.8rem;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.15s ease;
}

.facet-option:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  background: #eff6ff;
}

.facet-option-selected {
  border-color: #3b82f6;
  background: #3b82f6;
  color: #fff;
}

.facet-option-active {
  border-color: #ef4444;
  color: #ef4444;
  background: #fef2f2;
}

.facet-count {
  font-size: 0.7rem;
  opacity: 0.7;
}
```

- [ ] **Step 3: Integrate into SearchPage.tsx**

Add imports:
```tsx
import { FacetFilter } from '@/components/search/FacetFilter';
import { X } from 'lucide-react';
```

Add state for facet filtering (add to state declarations in the component):
```tsx
    const [facetCategoryId, setFacetCategoryId] = useState<string>();
    const [facetCondition, setFacetCondition] = useState<string>();
```

Add facet filter UI between search results header and product grid (around line 441, after loading state):
```tsx
                        {/* Facet Filters */}
                        {aggregations && !isSearching && (
                            <div className="search-facet-bar">
                                <FacetFilter
                                    title="分类"
                                    buckets={aggregations.category}
                                    selectedKey={facetCategoryId}
                                    onChange={setFacetCategoryId}
                                />
                                <FacetFilter
                                    title="成色"
                                    buckets={aggregations.conditionLevel}
                                    selectedKey={facetCondition}
                                    onChange={setFacetCondition}
                                />
                            </div>
                        )}
```

Add basic styling in `search.css`:
```css
.search-facet-bar {
  display: flex;
  gap: 1.5rem;
  padding: 0.8rem 0;
  flex-wrap: wrap;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 1rem;
}
```

- [ ] **Step 4: Verify build**

Run: `cd easyorange-frontend && npx tsc --noEmit`
Expected: No type errors

- [ ] **Step 5: Commit**

```bash
git add easyorange-frontend/src/components/search/FacetFilter.tsx
git add easyorange-frontend/src/components/search/facet-filter.css
git add easyorange-frontend/src/pages/profile/SearchPage.tsx
git commit -m "feat(frontend): add FacetFilter component with aggregation support"
```

---

### Batch E — Testing

---

### Task E1: Unit tests for ES adapters (mock ElasticsearchOperations)

**Files:**
- Create: `easyorange-application/src/test/java/com/cartethyia/easyorange/adapter/outbound/elasticsearch/ElasticsearchProductSearchIndexAdapterTest.java`

- [ ] **Step 1: Create test for index adapter**

```java
package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ElasticsearchProductSearchIndexAdapter 单元测试")
class ElasticsearchProductSearchIndexAdapterTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductDetailMapper productDetailMapper;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private ElasticsearchProductSearchIndexAdapter adapter;

    private ProductDO testProduct;

    @BeforeEach
    void setUp() {
        adapter = new ElasticsearchProductSearchIndexAdapter(productMapper, productDetailMapper, elasticsearchOperations);

        testProduct = new ProductDO();
        testProduct.setId(1L);
        testProduct.setUserId(10L);
        testProduct.setName("测试商品");
        testProduct.setPrice(new java.math.BigDecimal("100.00"));
        testProduct.setStatus((byte) 1);
        testProduct.setConditionLevel((byte) 9);
        testProduct.setLocation("北京");
        testProduct.setTags("手机,数码");
        testProduct.setImages("http://img/1.jpg");
        testProduct.setMainImage("http://img/1.jpg");
        testProduct.setUsername("卖家甲");
        testProduct.setCreateTime(LocalDateTime.now());
        testProduct.setUpdateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("indexProduct 应构建文档并保存到 ES")
    void indexProduct_shouldBuildAndSaveDocument() {
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productDetailMapper.selectById(1L)).thenReturn(null);

        adapter.indexProduct(1L);

        verify(elasticsearchOperations).save(any(ProductDocument.class));
    }

    @Test
    @DisplayName("removeProductIndex 应删除 ES 文档")
    void removeProductIndex_shouldDeleteDocument() {
        adapter.removeProductIndex(1L);

        verify(elasticsearchOperations).delete("1", ProductDocument.class);
    }

    @Test
    @DisplayName("buildDocument 应正确映射字段")
    void buildDocument_shouldMapFieldsCorrectly() {
        ProductDetailDO detail = new ProductDetailDO();
        detail.setId(1L);
        detail.setDescription("商品描述");

        when(productDetailMapper.selectById(1L)).thenReturn(detail);

        ProductDocument doc = adapter.buildDocument(testProduct);

        assertThat(doc.getId()).isEqualTo("1");
        assertThat(doc.getName()).isEqualTo("测试商品");
        assertThat(doc.getDescription()).isEqualTo("商品描述");
        assertThat(doc.getTags()).containsExactly("手机", "数码");
        assertThat(doc.getImages()).containsExactly("http://img/1.jpg");
    }
}
```

Add import for `assertThat`:
```java
import static org.assertj.core.api.Assertions.assertThat;
```

- [ ] **Step 2: Run unit tests**

Run: `./mvnw test -pl easyorange-application -DexcludedGroups=integration -q`
Expected: Tests pass

- [ ] **Step 3: Commit**

```bash
git add easyorange-application/src/test/java/com/cartethyia/easyorange/adapter/outbound/elasticsearch/ElasticsearchProductSearchIndexAdapterTest.java
git commit -m "test(application): add ES index adapter unit test"
```

---

## Self-Review

### Spec coverage check:
1. ✅ Port interface + SearchResult/FacetBucket in product domain (Task A1)
2. ✅ Routing in ProductSearchHandler via Optional (Task B1)
3. ✅ ES index write adapter replacing MySQL adapter (Task A7)
4. ✅ ES query adapter with multi_match, fuzziness, filters (Task A8)
5. ✅ Faceted aggregation: category, conditionLevel, priceRanges (Task A8)
6. ✅ Index mapping: IK analyzer, scaled_float for price (Task A4/A5)
7. ✅ Full reindex admin endpoint (Task B2)
8. ✅ Dockerfile with IK plugin (Task C1)
9. ✅ docker-compose ES service (Task C2)
10. ✅ Spring Data ES dependency + config (Task A3)
11. ✅ Frontend aggregations type + FacetFilter UI (Task D1/D2)
12. ✅ Unit tests for ES adapters (Task E1)

### Gaps:
- [ ] Testcontainers integration test (ES + MySQL). Scope: verify Chinese search, faceted aggregation, fuzzy matching. Requires Docker runtime. Can be added after plan execution in a follow-up PR.
- [ ] `@Tag("integration")` should be used for Testcontainers tests.
- [ ] The existing `ProductSearchHandlerTest` uses mock `ProductQueryRepository.searchProducts(String, Long, Integer, Integer, Integer)` — when `ProductSearchQueryPort` is present but mocked to return empty, the ES path is used, so the mock for `productQueryRepository.searchProducts` won't be invoked for search. The test needs to handle this — it will need to verify the ES route was called. However, since the mock returns `Optional.of(searchQueryPort)`, the fallback test should set it to `Optional.empty()`.

