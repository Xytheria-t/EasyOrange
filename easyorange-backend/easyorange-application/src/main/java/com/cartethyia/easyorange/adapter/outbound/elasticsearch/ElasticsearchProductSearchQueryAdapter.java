package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.domain.port.output.FacetBucket;
import com.cartethyia.easyorange.product.domain.port.output.ProductSearchQueryPort;
import com.cartethyia.easyorange.product.domain.port.output.SearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ElasticsearchProductSearchQueryAdapter implements ProductSearchQueryPort {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;

    @Override
    public SearchResult search(ProductSearchQuery query) {
        int page = Math.max(query.pageNum(), 1);
        int size = Math.max(query.pageSize(), 1);

        // Build the search JSON (query + sort + aggregations only; pagination via PageRequest)
        ObjectNode root = objectMapper.createObjectNode();
        root.set("query", buildQuery(query));
        root.set("sort", buildSort(query.sort()));
        root.set("aggs", buildAggregations());

        String queryJson = root.toString();

        // Execute search
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(
                new StringQuery(queryJson, PageRequest.of(page - 1, size)),
                ProductDocument.class
        );

        // Convert documents
        List<ProductReadModel> records = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toReadModel)
                .collect(Collectors.toList());

        // Extract aggregations
        List<FacetBucket> categoryFacets = extractAggBuckets(searchHits, "category");
        List<FacetBucket> conditionFacets = extractAggBuckets(searchHits, "conditionLevel");
        List<FacetBucket> priceRangeFacets = extractRangeAggBuckets(searchHits, "priceRanges");

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
        ArrayNode filter = objectMapper.createArrayNode();
        if (query.status() != null) {
            filter.add(objectMapper.createObjectNode().set("term",
                    objectMapper.createObjectNode().put("status", query.status())));
        }
        if (query.categoryId() != null) {
            filter.add(objectMapper.createObjectNode().set("term",
                    objectMapper.createObjectNode().put("categoryId", query.categoryId())));
        }
        if (query.conditionLevel() != null) {
            filter.add(objectMapper.createObjectNode().set("term",
                    objectMapper.createObjectNode().put("conditionLevel", query.conditionLevel())));
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

        if (filter.size() > 0) {
            bool.set("filter", filter);
        }

        return objectMapper.createObjectNode().set("bool", bool);
    }

    private ArrayNode buildSort(String sortField) {
        ArrayNode sort = objectMapper.createArrayNode();
        String sortKey = sortField != null ? sortField : "relevance";
        switch (sortKey) {
            case "price_asc" -> sort.add(objectMapper.createObjectNode().set("price",
                    objectMapper.createObjectNode().put("order", "asc")));
            case "price_desc" -> sort.add(objectMapper.createObjectNode().set("price",
                    objectMapper.createObjectNode().put("order", "desc")));
            case "newest" -> sort.add(objectMapper.createObjectNode().set("createTime",
                    objectMapper.createObjectNode().put("order", "desc")));
            case "popular" -> sort.add(objectMapper.createObjectNode().set("viewCount",
                    objectMapper.createObjectNode().put("order", "desc")));
            default -> sort.add(objectMapper.createObjectNode().set("_score",
                    objectMapper.createObjectNode().put("order", "desc")));
        }
        return sort;
    }

    private ObjectNode buildAggregations() {
        ObjectNode aggs = objectMapper.createObjectNode();

        // category terms
        ObjectNode categoryAgg = objectMapper.createObjectNode();
        categoryAgg.set("terms", objectMapper.createObjectNode()
                .put("field", "categoryId")
                .put("size", 20));
        aggs.set("category", categoryAgg);

        // conditionLevel terms
        ObjectNode conditionAgg = objectMapper.createObjectNode();
        conditionAgg.set("terms", objectMapper.createObjectNode()
                .put("field", "conditionLevel")
                .put("size", 10));
        aggs.set("conditionLevel", conditionAgg);

        // priceRanges range
        ObjectNode priceRanges = objectMapper.createObjectNode();
        ObjectNode rangeField = objectMapper.createObjectNode();
        rangeField.put("field", "price");
        ArrayNode ranges = rangeField.putArray("ranges");
        ranges.add(objectMapper.createObjectNode().put("to", 100).put("key", "*-100"));
        ranges.add(objectMapper.createObjectNode().put("from", 100).put("to", 500).put("key", "100-500"));
        ranges.add(objectMapper.createObjectNode().put("from", 500).put("to", 1000).put("key", "500-1000"));
        ranges.add(objectMapper.createObjectNode().put("from", 1000).put("key", "1000-*"));
        priceRanges.set("range", rangeField);
        aggs.set("priceRanges", priceRanges);

        return aggs;
    }

    private ProductReadModel toReadModel(ProductDocument doc) {
        return new ProductReadModel(
                doc.getId() != null ? Long.parseLong(doc.getId()) : null,
                doc.getUserId(),
                null,                                    // username
                null,                                    // userAvatar
                null,                                    // categoryId (Long — stored as Integer in ES)
                doc.getCategoryName(),
                doc.getName(),                           // title
                doc.getDescription(),
                doc.getPrice() != null ? BigDecimal.valueOf(doc.getPrice()) : null,
                doc.getOriginalPrice() != null ? BigDecimal.valueOf(doc.getOriginalPrice()) : null,
                doc.getStock(),
                doc.getStatus() != null ? doc.getStatus().intValue() : null,
                null,                                    // statusDesc
                doc.getViewCount(),
                doc.getConditionLevel() != null ? doc.getConditionLevel().intValue() : null,
                null,                                    // conditionDesc
                doc.getLocation(),
                null,                                    // contactMethod
                doc.getImages(),
                doc.getMainImage(),
                doc.getCreateTime(),
                doc.getUpdateTime()
        );
    }

    private List<FacetBucket> extractAggBuckets(SearchHits<?> searchHits, String aggName) {
        if (searchHits.getAggregations() == null) return List.of();

        var aggsContainer = (ElasticsearchAggregations) searchHits.getAggregations();
        ElasticsearchAggregation agg = aggsContainer.get(aggName);
        if (agg == null) return List.of();

        var aggregate = agg.aggregation().getAggregate();

        // Try string terms (keyword fields)
        var sterms = aggregate.sterms();
        if (sterms != null && sterms.buckets() != null) {
            return sterms.buckets().array().stream()
                    .map(b -> new FacetBucket(b.key().stringValue(), b.key().stringValue(), b.docCount()))
                    .collect(Collectors.toList());
        }

        // Try long terms (integer fields — categoryId, conditionLevel are integers)
        var lterms = aggregate.lterms();
        if (lterms != null && lterms.buckets() != null) {
            return lterms.buckets().array().stream()
                    .map(b -> new FacetBucket(String.valueOf(b.key()), String.valueOf(b.key()), b.docCount()))
                    .collect(Collectors.toList());
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
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
