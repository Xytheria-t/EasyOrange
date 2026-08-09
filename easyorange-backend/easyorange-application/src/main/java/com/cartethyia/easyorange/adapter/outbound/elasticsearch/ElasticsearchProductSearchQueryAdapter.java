package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;
import com.cartethyia.easyorange.product.application.port.query.FacetBucket;
import com.cartethyia.easyorange.product.application.port.query.ProductSearchQueryPort;
import com.cartethyia.easyorange.product.application.port.query.SearchResult;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.Queries;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Slf4j
@Component
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
@Primary
public class ElasticsearchProductSearchQueryAdapter implements ProductSearchQueryPort {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;

    @Override
    public SearchResult search(ProductSearchQuery query) {
        int page = Math.max(query.pageNum(), 1);
        int size = Math.max(query.pageSize(), 1);

        boolean useKnn = query.useSemanticSearch()
                && query.queryEmbedding() != null
                && !query.queryEmbedding().isEmpty();

        var queryBuilder = NativeQuery.builder().withPageable(PageRequest.of(page - 1, size));

        if (useKnn) {
            queryBuilder.withKnnSearches(knn -> knn.field("nameEmbedding")
                    .queryVector(query.queryEmbedding())
                    .k(size * 2)
                    .numCandidates(100));
            queryBuilder.withQuery(
                    Queries.wrapperQueryAsQuery(buildFilterQuery(query).toString()));
        } else {
            queryBuilder.withQuery(Queries.wrapperQueryAsQuery(buildQuery(query).toString()));
        }

        queryBuilder.withSort(sortOptions(query.sort()));
        queryBuilder.withAggregation("category", categoryAgg());
        queryBuilder.withAggregation("conditionLevel", conditionAgg());
        queryBuilder.withAggregation("priceRanges", priceRangeAgg());

        // 用 NativeQuery 组装请求体：SDE 的 StringQuery 会把整段 body 当作 query DSL 包进 wrapper 查询，
        // 形成 {"query":{"query":…,"sort":…,"aggs":…}} 被 ES 拒收（unknown query [query]）。
        SearchHits<ProductDocument> searchHits =
                elasticsearchOperations.search(queryBuilder.build(), ProductDocument.class);

        // Convert documents
        List<ProductReadModel> records = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toReadModel)
                .toList();

        // Extract aggregations
        List<FacetBucket> categoryFacets = extractAggBuckets(searchHits, "category");
        List<FacetBucket> conditionFacets = extractAggBuckets(searchHits, "conditionLevel");
        List<FacetBucket> priceRangeFacets = extractRangeAggBuckets(searchHits, "priceRanges");

        return new SearchResult(
                records, searchHits.getTotalHits(), page, size, categoryFacets, conditionFacets, priceRangeFacets);
    }

    private JsonNode buildQuery(ProductSearchQuery query) {
        ObjectNode bool = objectMapper.createObjectNode();

        // Must clause
        ArrayNode must = objectMapper.createArrayNode();
        String keyword = query.keyword();
        if (keyword != null && !keyword.isBlank()) {
            ObjectNode multiMatch = objectMapper.createObjectNode();
            multiMatch.put("query", keyword);
            multiMatch.put("type", "best_fields");
            multiMatch.put("fuzziness", "AUTO");
            ArrayNode fields = multiMatch.putArray("fields");
            fields.add("name^3");
            fields.add("description");
            must.add(objectMapper.createObjectNode().set("multi_match", multiMatch));
        } else {
            must.add(objectMapper.createObjectNode().set("match_all", objectMapper.createObjectNode()));
        }
        bool.set("must", must);

        // Filter clauses
        ArrayNode filter = buildFilterClauses(query);
        if (filter.size() > 0) {
            bool.set("filter", filter);
        }

        return objectMapper.createObjectNode().set("bool", bool);
    }

    private JsonNode buildFilterQuery(ProductSearchQuery query) {
        ObjectNode bool = objectMapper.createObjectNode();

        ArrayNode must = objectMapper.createArrayNode();
        must.add(objectMapper.createObjectNode().set("match_all", objectMapper.createObjectNode()));
        bool.set("must", must);

        ArrayNode filter = buildFilterClauses(query);
        if (filter.size() > 0) {
            bool.set("filter", filter);
        }

        return objectMapper.createObjectNode().set("bool", bool);
    }

    /** 复用过滤子句（status/categoryId/conditionLevel/price），供全文检索与语义 kNN 共用，避免重复漂移 */
    private ArrayNode buildFilterClauses(ProductSearchQuery query) {
        ArrayNode filter = objectMapper.createArrayNode();
        if (query.status() != null) {
            filter.add(objectMapper
                    .createObjectNode()
                    .set("term", objectMapper.createObjectNode().put("status", query.status())));
        }
        if (query.categoryId() != null) {
            filter.add(objectMapper
                    .createObjectNode()
                    .set("term", objectMapper.createObjectNode().put("categoryId", query.categoryId())));
        }
        if (query.conditionLevel() != null) {
            filter.add(objectMapper
                    .createObjectNode()
                    .set("term", objectMapper.createObjectNode().put("conditionLevel", query.conditionLevel())));
        }
        if (query.minPrice() != null || query.maxPrice() != null) {
            ObjectNode range = objectMapper.createObjectNode();
            ObjectNode priceRange = objectMapper.createObjectNode();
            if (query.minPrice() != null) {
                priceRange.put("gte", query.minPrice());
            }
            if (query.maxPrice() != null) {
                priceRange.put("lte", query.maxPrice());
            }
            range.set("price", priceRange);
            filter.add(objectMapper.createObjectNode().set("range", range));
        }

        return filter;
    }

    private List<SortOptions> sortOptions(String sortField) {
        String sortKey = sortField != null ? sortField : "relevance";
        return switch (sortKey) {
            case "price_asc" ->
                List.of(SortOptions.of(so -> so.field(f -> f.field("price").order(SortOrder.Asc))));
            case "price_desc" ->
                List.of(SortOptions.of(so -> so.field(f -> f.field("price").order(SortOrder.Desc))));
            case "newest" ->
                List.of(SortOptions.of(so -> so.field(f -> f.field("createTime").order(SortOrder.Desc))));
            case "popular" ->
                List.of(SortOptions.of(so -> so.field(f -> f.field("viewCount").order(SortOrder.Desc))));
            default -> List.of(SortOptions.of(so -> so.score(s -> s.order(SortOrder.Desc))));
        };
    }

    private Aggregation categoryAgg() {
        return Aggregation.of(a -> a.terms(t -> t.field("categoryId").size(20)));
    }

    private Aggregation conditionAgg() {
        return Aggregation.of(a -> a.terms(t -> t.field("conditionLevel").size(10)));
    }

    private Aggregation priceRangeAgg() {
        return Aggregation.of(a -> a.range(r -> r.field("price")
                .ranges(
                        AggregationRange.of(rr -> rr.to(100d).key("*-100")),
                        AggregationRange.of(rr -> rr.from(100d).to(500d).key("100-500")),
                        AggregationRange.of(rr -> rr.from(500d).to(1000d).key("500-1000")),
                        AggregationRange.of(rr -> rr.from(1000d).key("1000-*")))));
    }

    private ProductReadModel toReadModel(ProductDocument doc) {
        return ProductReadModel.builder()
                .id(doc.getId())
                .sellerId(doc.getUserId() != null ? doc.getUserId().toString() : null)
                .categoryId(doc.getCategoryId())
                .categoryName(doc.getCategoryName())
                .title(doc.getName())
                .description(doc.getDescription())
                .price(doc.getPrice() != null ? BigDecimal.valueOf(doc.getPrice()) : null)
                .originalPrice(doc.getOriginalPrice() != null ? BigDecimal.valueOf(doc.getOriginalPrice()) : null)
                .stock(doc.getStock())
                .status(doc.getStatus())
                .views(doc.getViewCount())
                .condition(doc.getConditionLevel())
                .location(doc.getLocation())
                .images(Objects.requireNonNullElseGet(doc.getImages(), List::of))
                .mainImageUrl(Objects.requireNonNullElse(doc.getMainImage(), ""))
                .createTime(fromEpochMillis(doc.getCreateTime()))
                .updateTime(fromEpochMillis(doc.getUpdateTime()))
                .build();
    }

    /** epoch millis → LocalDateTime（与索引写入 side 的 {@code toEpochMillis} 互逆，同一系统时区口径） */
    private static LocalDateTime fromEpochMillis(Long epochMillis) {
        if (epochMillis == null) {
            return null;
        }
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private List<FacetBucket> extractAggBuckets(SearchHits<?> searchHits, String aggName) {
        if (searchHits.getAggregations() == null) return List.of();

        var aggsContainer = (ElasticsearchAggregations) searchHits.getAggregations();
        ElasticsearchAggregation agg = aggsContainer.get(aggName);
        if (agg == null) return List.of();

        var aggregate = agg.aggregation().getAggregate();

        // 先按变体类型判空再取值：Aggregate.sterms()/lterms() 在变体不匹配时抛 IllegalStateException（不返回 null）
        if (aggregate.isSterms()) {
            var sterms = aggregate.sterms();
            if (sterms != null && sterms.buckets() != null) {
                return sterms.buckets().array().stream()
                        .map(b -> new FacetBucket(b.key().stringValue(), b.key().stringValue(), b.docCount()))
                        .toList();
            }
        }

        // Long terms（integer 字段 — categoryId/conditionLevel 映射为数字类型，bucket.key() 为原始 long）
        if (aggregate.isLterms()) {
            var lterms = aggregate.lterms();
            if (lterms != null && lterms.buckets() != null) {
                return lterms.buckets().array().stream()
                        .map(b -> new FacetBucket(String.valueOf(b.key()), String.valueOf(b.key()), b.docCount()))
                        .toList();
            }
        }

        return List.of();
    }

    private List<FacetBucket> extractRangeAggBuckets(SearchHits<?> searchHits, String aggName) {
        if (searchHits.getAggregations() == null) return List.of();

        var aggsContainer = (ElasticsearchAggregations) searchHits.getAggregations();
        ElasticsearchAggregation agg = aggsContainer.get(aggName);
        if (agg == null) return List.of();

        var aggregate = agg.aggregation().getAggregate();
        var rangeAgg = aggregate.range();
        if (rangeAgg != null && rangeAgg.buckets() != null) {
            return rangeAgg.buckets().array().stream()
                    .map(b -> {
                        String key = b.key() != null ? b.key() : (b.from() + "-" + b.to());
                        return new FacetBucket(key, key, b.docCount());
                    })
                    .toList();
        }

        return List.of();
    }
}
