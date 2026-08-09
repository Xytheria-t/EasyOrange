package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.adapter.outbound.elasticsearch.ElasticsearchProductSearchQueryAdapter;
import com.cartethyia.easyorange.adapter.outbound.elasticsearch.ProductDocument;
import com.cartethyia.easyorange.product.application.port.query.ProductSearchQueryPort.ProductSearchQuery;
import com.cartethyia.easyorange.product.application.port.query.SearchResult;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;

/**
 * 商品搜索真实 ES 链路测试 —— 这是全 Mockito 单测覆盖不到的关键安全网：
 * Spring Data ES 实体 → 请求体序列化 → 真实 ES 索引映射的完整兼容性。
 *
 * <p>它一次性兜住三类此前被 mocks 放跑的错位：
 * <ul>
 *   <li>时间字段：无注解 {@code LocalDateTime} 会被 SDE 写成 ISO-8601（含 'T'/UTC），
 *       与 JSON mapping 的 date 格式不符会整篇拒收——本测试写入 epoch_millis 并读回日期，验证映射自洽；</li>
 *   <li>枚举字段：status 存的是 {@code "ONLINE"} 这类 code 字符串，映射必须为 keyword，否则 term 查询解析失败；</li>
 *   <li>IK 分词插件缺失会导致 {@code ElasticsearchIndexManager} 启动建索引失败，上下文起不来即当场暴露。</li>
 * </ul>
 *
 * <p>激活 profile {@code it-es}（{@code application-it-es.yaml}）：测试自行拉起 compose 的
 * mysql/redis/rabbitmq/elasticsearch（start-only），再把 {@code easyorange.search.elasticsearch.enabled} 打开。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"it", "it-es"})
class ElasticsearchProductSearchIT {

    private static final String DOC_ID_1 = "it-es-doc-1";
    private static final String DOC_ID_2 = "it-es-doc-2";

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private ElasticsearchProductSearchQueryAdapter queryAdapter;

    @Test
    @DisplayName("写入→查询全链路：索引存在、keyword 命中、status/价格过滤、newest 排序、facet、日期读回")
    void indexWriteAndSearchRoundTrip() {
        // 启动时 ElasticsearchIndexManager 已按 JSON mapping/settings 建好索引（含 IK 分词与 dense_vector）
        var indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
        assertThat(indexOps.exists()).as("products 索引应在启动时由 JSON mapping 建立").isTrue();

        try {
            var newer = LocalDateTime.of(2026, 8, 10, 12, 0, 0);
            var older = LocalDateTime.of(2026, 8, 9, 10, 15, 30);

            save(DOC_ID_1, "iPhone 14 Pro 深蓝色 手机", "9成新 无拆修", older, 5999.0, 5, 120);
            save(DOC_ID_2, "iPhone 14 白色 手机", "全新未拆封", newer, 6999.0, 0, 300);
            elasticsearchOperations.indexOps(ProductDocument.class).refresh();

            // keyword 命中 + newest 排序（createTime 为 epoch_millis，数字排序不涉时区）
            var keywordHit = queryAdapter.search(
                    new ProductSearchQuery("iPhone", null, null, null, null, null, "newest", 1, 20, null, false));
            assertThat(keywordHit.records()).extracting(ProductReadModel::id).contains(DOC_ID_1, DOC_ID_2);
            assertThat(keywordHit.total()).isEqualTo(2);
            assertThat(keywordHit.records())
                    .as("newest 应将 createTime 更晚的 DOC_ID_2 排最前")
                    .first()
                    .extracting(ProductReadModel::id)
                    .isEqualTo(DOC_ID_2);

            // status 过滤：ONLINE 命中文档，OFFLINE 应返回空
            var onlineOnly = search(keyword("iPhone", null));
            assertThat(onlineOnly.records()).hasSize(2);
            var offlineOnly = search(
                    new ProductSearchQuery("iPhone", null, "OFFLINE", null, null, null, null, 1, 20, null, false));
            assertThat(offlineOnly.records()).isEmpty();

            // 价格范围过滤
            var inRange = search(new ProductSearchQuery(
                    null,
                    null,
                    null,
                    BigDecimal.valueOf(6000),
                    BigDecimal.valueOf(8000),
                    null,
                    null,
                    1,
                    20,
                    null,
                    false));
            assertThat(inRange.records()).extracting(ProductReadModel::id).containsExactly(DOC_ID_2);

            // facet：category 聚合应能读出桶
            var all = search(new ProductSearchQuery(null, null, null, null, null, null, null, 1, 20, null, false));
            assertThat(all.categoryFacets()).as("品类 facet 应从 ES terms 聚合读出").isNotEmpty();

            // 日期读回：createTime 非空（epoch_millis 序列化 + 反序列化通路成立）
            var readBack = search(keyword("无拆修", null));
            assertThat(readBack.records()).hasSize(1);
            assertThat(readBack.records().get(0).createTime()).isNotNull();
        } finally {
            elasticsearchOperations.delete(DOC_ID_1, ProductDocument.class);
            elasticsearchOperations.delete(DOC_ID_2, ProductDocument.class);
        }
    }

    private SearchResult search(ProductSearchQuery query) {
        return queryAdapter.search(query);
    }

    private ProductSearchQuery keyword(String keyword, String sort) {
        return new ProductSearchQuery(keyword, null, null, null, null, null, sort, 1, 20, null, false);
    }

    private void save(
            String id,
            String name,
            String description,
            LocalDateTime createTime,
            double price,
            int stock,
            int viewCount) {
        var doc = ProductDocument.builder()
                .id(id)
                .userId("u-it")
                .name(name)
                .description(description)
                .categoryId("3")
                .categoryName("手机数码")
                .price(price)
                .originalPrice(price + 1000)
                .conditionLevel("2")
                .status("ONLINE")
                .viewCount(viewCount)
                .stock(stock)
                .location("上海市")
                .tags(List.of("手机", "数码"))
                .mainImage("https://img.example.com/it.jpg")
                .images(List.of("https://img.example.com/it.jpg"))
                .createTime(toEpochMillis(createTime))
                .updateTime(toEpochMillis(createTime))
                .build();
        elasticsearchOperations.save(doc);
    }

    /** LocalDateTime → epoch millis（与 {@code ElasticsearchProductSearchIndexAdapter.toEpochMillis} 同口径） */
    private static Long toEpochMillis(LocalDateTime value) {
        return value.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
