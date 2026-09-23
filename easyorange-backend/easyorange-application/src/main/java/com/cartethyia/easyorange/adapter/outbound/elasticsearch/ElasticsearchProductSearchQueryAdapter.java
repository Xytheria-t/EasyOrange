package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;
import com.cartethyia.easyorange.ai.domain.model.RrfFusion;
import com.cartethyia.easyorange.product.application.port.cache.SellerCachePort;
import com.cartethyia.easyorange.product.application.port.query.FacetBucket;
import com.cartethyia.easyorange.product.application.port.query.ProductSearchQueryPort;
import com.cartethyia.easyorange.product.application.port.query.SearchResult;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.SourceFilter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 商品检索（ES 适配器）— 实现 {@link ProductSearchQueryPort}。
 * <p>
 * <b>与 {@link AssetElasticsearchAdapter} / {@link KnowledgeElasticsearchAdapter} 刻意对称：两路独立召回 + RRF 排名融合。</b>
 * kNN（{@code nameEmbedding}，dense_vector）与 BM25（{@code multi_match name^3/description}）各查一次，
 * 再用 {@link RrfFusion} 按排名融合 —— 同一请求里「knn + query」不行：ES 会把两路分数按内部规则合成一个分值，
 * 拿不到各自排名，而余弦相似度与 BM25 分值量纲不可比，只有排名能融（ADR-0012）。
 * <p>
 * <b>何时走两路</b>：仅当「按相关性排序」且拿得到查询向量（见 {@link #useTwoLegRecall}）。
 * 显式排序（价格 / 最新 / 热度）走单路 BM25 并保留原来的 ES 侧分页与排序；关键词为空同样单路 ——
 * kNN 缺 query 子句只能退化成 match_all，等于按过滤条件随机取一批。
 * <p>
 * <b>单路失败不影响另一路</b>：任一路抛异常就退化为另一路单独排序（与 AI 助手找货链路同一语义），
 * 两路都失败才回落到单路查询。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
@Primary
public class ElasticsearchProductSearchQueryAdapter implements ProductSearchQueryPort {

    /**
     * 两路召回各取的候选条数 —— <b>固定值</b>，不随页码增长。
     * <p>
     * 融合会把候选池重新排序，池子大小随页码变化会让「同一查询翻到第 2 页时，第 1 页的顺序也变了」
     * （原本排在池子外的文档在更大的池子里融合分变高）。固定池子换来翻页稳定，代价是超过
     * {@code 候选池 / pageSize} 页之后不再有新的语义候选 —— 搜索页深度翻页本就很少，这个取舍划算。
     */
    /**
     * 两路召回各取的候选条数下限（≈5 页 @20 条）。
     * <p>
     * 实际池子取 {@code max(下限, page * size * 2)}：下限保证了前几页的融合顺序稳定
     * （池子大小不变 ⇒ 同一查询的前几页排序不会因为翻页而重排），
     * 随页码增长则保证**请求的那一页永远落在池内** —— 固定池子在深翻页时会返回空记录，
     * 而总数仍报着 BM25 的完整计数，前后端就对不上了。
     */
    private static final int MIN_CANDIDATES = 100;

    /**
     * kNN 的 {@code num_candidates} 下限：ES 先按 ANN 取这么多候选再精确打分（远大于 k 才有效果）。
     * <p>
     * 必须 ≥ {@code k} —— 否则 ES 直接拒收整个请求（{@code illegal_argument_exception:
     * [num_candidates] cannot be less than [k]}）。候选池取 200 时用固定的 100 会踩这个坑，
     * 所以这里取下限与 {@code 2k} 的较大者。
     */
    private static final int NUM_CANDIDATES = 100;

    /**
     * kNN 路的余弦相似度下限：低于该值的文档不进候选池，按 0 命中处理。
     * <p>
     * 纯 kNN 召回没有相关性门槛 —— 小语料下 ANN 会把全库都凑满 {@code k} 条，
     * 融合后不相关商品被顶进结果页。实测（2026-09-23，106 文档 / text-embedding-v3 + bbq_hnsw）：
     * 乱码查询 0 命中、「相机」4 条、自然语言查询 7 条 —— 门槛按 ANN 图上的估计分剪枝，
     * 报告的 {@code _score} 是 rescore 后的值，0.5 参数对应报告分约 0.75 的有效切点。
     * 只影响语义路，BM25 词面命中不受限。
     */
    private static final float KNN_MIN_SIMILARITY = 0.5f;

    /** 索引里只取检索需要的字段（1024 维向量不回传，只在 ES 内部参与打分）。 */
    private static final SourceFilter SOURCE_FILTER =
            new FetchSourceFilterBuilder().withExcludes("nameEmbedding").build();

    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;
    private final SellerCachePort sellerCachePort;

    @Override
    public SearchResult search(ProductSearchQuery query) {
        int page = Math.max(query.pageNum(), 1);
        int size = Math.max(query.pageSize(), 1);

        if (!useTwoLegRecall(query)) {
            return singleLegSearch(query, page, size);
        }
        return fusedSearch(query, page, size);
    }

    private static boolean useTwoLegRecall(ProductSearchQuery query) {
        return query.useSemanticSearch()
                && query.queryEmbedding() != null
                && !query.queryEmbedding().isEmpty()
                && ProductSearchQueryPort.isRelevanceSort(query.sort());
    }

    /** 单路 BM25：显式排序、关键词为空、以及两路都失败时都走这里，分页与排序完全交给 ES。 */
    private SearchResult singleLegSearch(ProductSearchQuery query, int page, int size) {
        var queryBuilder = NativeQuery.builder()
                .withPageable(PageRequest.of(page - 1, size))
                .withQuery(Queries.wrapperQueryAsQuery(buildQuery(query).toString()))
                .withSort(sortOptions(query.sort()))
                .withAggregation("category", categoryAgg())
                .withAggregation("conditionLevel", conditionAgg())
                .withAggregation("priceRanges", priceRangeAgg());

        // 用 NativeQuery 组装请求体：SDE 的 StringQuery 会把整段 body 当作 query DSL 包进 wrapper 查询，
        // 形成 {"query":{"query":…,"sort":…,"aggs":…}} 被 ES 拒收（unknown query [query]）。
        SearchHits<ProductDocument> searchHits =
                elasticsearchOperations.search(queryBuilder.build(), ProductDocument.class);

        return toResult(searchHits, page, size, searchHits.getTotalHits(), fillSellers(extractRecords(searchHits)));
    }

    /** 两路召回 + RRF 融合：候选池按需增长，融合后按页切片（顺序由排名决定，不由 ES 分页决定）。 */
    private SearchResult fusedSearch(ProductSearchQuery query, int page, int size) {
        int candidateK = Math.max(MIN_CANDIDATES, page * size * 2);
        var knnLeg = runLeg(knnQuery(query, candidateK));
        var bm25Leg = runLeg(bm25Query(query, candidateK));

        var docsById = new LinkedHashMap<String, ProductDocument>();
        var rankedLists = new ArrayList<List<String>>();
        if (!knnLeg.ids().isEmpty()) {
            docsById.putAll(knnLeg.docs());
            rankedLists.add(knnLeg.ids());
        }
        if (!bm25Leg.ids().isEmpty()) {
            docsById.putAll(bm25Leg.docs());
            rankedLists.add(bm25Leg.ids());
        }

        // 两路都空：可能是真无结果，也可能是 ES 抖动。交给单路查询再确认一次，
        // 避免把一次抖动放大成「搜索无结果」这种看起来正常的假象。
        if (rankedLists.isEmpty()) {
            return singleLegSearch(query, page, size);
        }

        // total 取 BM25 路的总命中：它是「过滤条件 + 关键词词面匹配」的完整计数，也是唯一有全量语义的口径
        // （kNN 只返回候选池条数，不是匹配总数）；BM25 路挂掉时退化为候选池大小。
        // 与融合后记录数取 max：语义路在词面命中之外补进来的召回也承诺给了用户（就记录在当前候选池里），
        // 只报 BM25 数会出现「共找到 4 件」却列出 10 张卡的口径裂缝。
        long bm25Total = bm25Leg.hits() != null ? bm25Leg.total() : 0L;

        var fused = RrfFusion.fuse(RrfFusion.DEFAULT_K, rankedLists);
        int from = Math.min((page - 1) * size, fused.size());
        int to = Math.min(from + size, fused.size());
        var records = fillSellers(fused.subList(from, to).stream()
                .map(f -> docsById.get(f.id()))
                .filter(Objects::nonNull)
                .map(this::toReadModel)
                .toList());

        // 词面命中为 0、但语义路召回到了结果时必须改报候选池大小：
        // 报 0 会让前端显示「共找到 0 件商品」却列着 N 张卡，同时翻页控件也消失
        long total = Math.max(bm25Total, fused.size());

        // facets 来自 BM25 那路的聚合：聚合是「过滤条件命中的语料」上的统计量，
        // 与融合后的排序无关，因此只需要一路带聚合，不必两路都算一遍。
        return toResult(bm25Leg.hits(), page, size, total, records);
    }

    private SearchResult toResult(
            SearchHits<ProductDocument> aggSource, int page, int size, long total, List<ProductReadModel> records) {
        if (aggSource == null) {
            return new SearchResult(records, total, page, size, List.of(), List.of(), List.of());
        }
        return new SearchResult(
                records,
                total,
                page,
                size,
                extractAggBuckets(aggSource, "category"),
                extractAggBuckets(aggSource, "conditionLevel"),
                extractRangeAggBuckets(aggSource, "priceRanges"));
    }

    private List<ProductReadModel> extractRecords(SearchHits<ProductDocument> hits) {
        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toReadModel)
                .toList();
    }

    /**
     * 批量补卖家展示信息（昵称/头像）：索引侧不含 sellerName，查询时经 {@link SellerCachePort}（Caffeine）回填。
     * 卖家服务不可用时降级为匿名展示，不影响检索主链路。
     */
    private List<ProductReadModel> fillSellers(List<ProductReadModel> records) {
        if (records.isEmpty()) {
            return records;
        }
        var sellerIds = records.stream()
                .map(ProductReadModel::sellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (sellerIds.isEmpty()) {
            return records;
        }
        Map<String, SellerReadModel> sellers;
        try {
            sellers = sellerCachePort.getSellers(sellerIds);
        } catch (Exception e) {
            log.warn("Seller info unavailable, search results fall back to anonymous seller", e);
            return records;
        }
        if (sellers == null || sellers.isEmpty()) {
            return records;
        }
        return records.stream()
                .map(r -> {
                    var seller = r.sellerId() != null ? sellers.get(r.sellerId()) : null;
                    if (seller == null) {
                        return r;
                    }
                    return r.toBuilder()
                            .username(seller.nickName() != null ? seller.nickName() : seller.username())
                            .userAvatar(seller.avatar())
                            .build();
                })
                .toList();
    }

    /** 单路召回结果：有序 ID + id → 文档（融合后按 id 回捞字段）+ 总命中数；失败时为空路且 {@code hits} 为 null。 */
    private record Leg(
            List<String> ids, Map<String, ProductDocument> docs, long total, SearchHits<ProductDocument> hits) {}

    private Leg runLeg(NativeQuery esQuery) {
        try {
            var hits = elasticsearchOperations.search(esQuery, ProductDocument.class);
            var ids = new ArrayList<String>(hits.getSearchHits().size());
            var docs = new LinkedHashMap<String, ProductDocument>();
            for (SearchHit<ProductDocument> hit : hits.getSearchHits()) {
                ids.add(hit.getId());
                docs.put(hit.getId(), hit.getContent());
            }
            return new Leg(ids, docs, hits.getTotalHits(), hits);
        } catch (Exception e) {
            // 单路失败不影响另一路：退化为单路排名，而不是把整个检索打成 500
            log.warn("Product search leg failed, falling back to single-leg ranking", e);
            return new Leg(List.of(), Map.of(), 0L, null);
        }
    }

    /**
     * kNN 腿：过滤条件放 {@code knn.filter}（预过滤），<b>不得挂顶层 query</b> ——
     * 顶层 query（match_all + 过滤）与 {@code knn.similarity} 并存时，kNN 侧被相似度剪成 0 后
     * ES 仍把 query 侧命中当结果返回（实测乱码查询该腿返 100 条、分值恒为 match_all 的 1.0），
     * 相似度门槛等于没有、任意乱码召回全库；过滤条件搬进 knn.filter 后同样查询 0 命中。
     * <p>
     * wrapper 查询装的是<b>查询对象</b> {@code {"bool":{"filter":[…]}}}——直接装子句数组
     * {@code [{…}]} 会被 ES 以 x_content_parse_exception 拒收，整条 kNN 腿静默退化成单路。
     */
    private NativeQuery knnQuery(ProductSearchQuery query, int k) {
        int numCandidates = Math.max(NUM_CANDIDATES, k * 2);
        var filterClauses = buildFilterClauses(query);
        return NativeQuery.builder()
                .withPageable(PageRequest.of(0, k))
                .withKnnSearches(knn -> {
                    knn.field("nameEmbedding")
                            .queryVector(query.queryEmbedding())
                            .k(k)
                            .numCandidates(numCandidates)
                            .similarity(KNN_MIN_SIMILARITY);
                    if (filterClauses.size() > 0) {
                        var filterQuery = objectMapper
                                .createObjectNode()
                                .set("bool", objectMapper.createObjectNode().set("filter", filterClauses));
                        knn.filter(Queries.wrapperQueryAsQuery(filterQuery.toString()));
                    }
                    return knn;
                })
                .withSourceFilter(SOURCE_FILTER)
                .withSort(byScoreDesc())
                .build();
    }

    private NativeQuery bm25Query(ProductSearchQuery query, int k) {
        return NativeQuery.builder()
                .withPageable(PageRequest.of(0, k))
                .withQuery(Queries.wrapperQueryAsQuery(buildQuery(query).toString()))
                .withSourceFilter(SOURCE_FILTER)
                .withSort(byScoreDesc())
                .withAggregation("category", categoryAgg())
                .withAggregation("conditionLevel", conditionAgg())
                .withAggregation("priceRanges", priceRangeAgg())
                .build();
    }

    private static List<SortOptions> byScoreDesc() {
        return List.of(SortOptions.of(so -> so.score(s -> s.order(SortOrder.Desc))));
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
            // 默认 OR 下多词查询任中一词即返回（「轻薄便携笔记本」混进充电器/羽绒服）；
            // ≤2 词仍要求全命中，≥3 词按 75% 向下取整（3 词命中 2 即可），词面召回保持宽容但不再是噪声
            multiMatch.put("minimum_should_match", "2<75%");
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

    /**
     * 过滤子句（status/categoryId/conditionLevel/price），两路召回共用。
     * <p>
     * <b>两路都必须带</b>：只过滤一路的话，不过滤的那路会把被过滤掉的商品带进候选池，
     * 融合后照样可能出现在结果里。kNN 路以 {@code knn.filter} 承载（见 {@link #knnQuery}）。
     */
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

    /**
     * 分类聚合：categoryId 分桶 + categoryName 子聚合取展示名 —— 前端筛选要传 id、展示要名称，两者都从桶里出。
     */
    private Aggregation categoryAgg() {
        return Aggregation.of(a -> a.terms(t -> t.field("categoryId").size(20))
                .aggregations(
                        "name", na -> na.terms(t -> t.field("categoryName").size(1))));
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
                // 索引侧字段名是 seller*，读模型/响应侧叫 username（前端归一为 sellerName）
                .username(doc.getSellerName())
                .userAvatar(doc.getSellerAvatar())
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
                .conditionDesc(ConditionLevelText.of(doc.getConditionLevel()))
                .location(doc.getLocation())
                // 索引侧 images 常缺省而 mainImage 恒有值：补位保证前端卡片取得到首图
                .images(
                        doc.getImages() != null && !doc.getImages().isEmpty()
                                ? doc.getImages()
                                : doc.getMainImage() != null ? List.of(doc.getMainImage()) : List.of())
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
                        .map(b -> new FacetBucket(
                                b.key().stringValue(),
                                subAggLabel(b.aggregations(), b.key().stringValue()),
                                b.docCount()))
                        .toList();
            }
        }

        // Long terms（integer 字段 — categoryId/conditionLevel 映射为数字类型，bucket.key() 为原始 long）
        if (aggregate.isLterms()) {
            var lterms = aggregate.lterms();
            if (lterms != null && lterms.buckets() != null) {
                return lterms.buckets().array().stream()
                        .map(b -> new FacetBucket(
                                String.valueOf(b.key()),
                                subAggLabel(b.aggregations(), String.valueOf(b.key())),
                                b.docCount()))
                        .toList();
            }
        }

        return List.of();
    }

    /** 分类桶的展示名来自 categoryName 子聚合；无子聚合的 agg（成色）回退为桶 key 本身。 */
    private static String subAggLabel(Map<String, Aggregate> subAggs, String fallback) {
        if (subAggs == null) {
            return fallback;
        }
        Aggregate nameAgg = subAggs.get("name");
        if (nameAgg == null || !nameAgg.isSterms()) {
            return fallback;
        }
        var aggregate = nameAgg.sterms();
        if (aggregate != null
                && aggregate.buckets() != null
                && !aggregate.buckets().array().isEmpty()) {
            return aggregate.buckets().array().get(0).key().stringValue();
        }
        return fallback;
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
                    // ES range agg 固定返回声明的全部区间：空桶（count=0）对用户是噪音「¥500-¥1000 0」，不下发
                    .filter(b -> b.docCount() > 0)
                    .map(b -> {
                        String key = b.key() != null ? b.key() : (b.from() + "-" + b.to());
                        return new FacetBucket(key, key, b.docCount());
                    })
                    .toList();
        }

        return List.of();
    }
}
