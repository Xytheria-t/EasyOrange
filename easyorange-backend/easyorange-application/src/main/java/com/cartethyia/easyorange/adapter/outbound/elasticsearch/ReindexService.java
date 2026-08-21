package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

/**
 * 全量重建 ES 商品索引服务。
 * 仅在 ES 启用时注册。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ReindexService {

    /** 单页商品数：全量重建按页游标推进，避免一次性加载全部在线商品 ID。 */
    private static final long REINDEX_PAGE_SIZE = 500;

    private final ProductMapper productMapper;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchIndexManager indexManager;
    private final ElasticsearchProductSearchIndexAdapter indexAdapter;

    /**
     * 全量重建索引：删除旧索引 → 按 JSON 配置重建索引 → 分页批量写入在线商品。
     *
     * <p>删除后必须先按 {@code product-mapping.json}/{@code product-settings.json} 重建索引：
     * 否则 {@code ElasticsearchOperations.save} 会触发 ES 动态 auto-create 出错误的动态 mapping
     * （丢失 IK analyzer、dense_vector 等字段定义），后续语义/分析全部失效。</p>
     *
     * <p>写入按 {@value REINDEX_PAGE_SIZE} 条/页游标推进并走
     * {@link ElasticsearchProductSearchIndexAdapter#indexProducts} 的批量预加载路径，
     * 避免逐商品 N+1（每个商品 4 次关联查询）与全量 ID 一次性驻留内存。</p>
     */
    public int reindexAll() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexManager.createProductIndex();

        int total = 0;
        long pageNum = 1;
        while (true) {
            // 字符串列 QueryWrapper + selectPage：纯 mock 单测环境无 MyBatis-Plus lambda 表缓存，
            // lambda select 急切解析会炸；字符串列等价且分页游标推进（search=false 免 COUNT）
            List<String> batchIds = productMapper
                    .selectPage(
                            new Page<>(pageNum, REINDEX_PAGE_SIZE, false),
                            new QueryWrapper<ProductDO>()
                                    .select("id")
                                    .eq("status", ProductStatus.ONLINE.getCode())
                                    .eq("del_flag", 0))
                    .getRecords()
                    .stream()
                    .map(ProductDO::getId)
                    .toList();
            if (batchIds.isEmpty()) {
                break;
            }
            indexAdapter.indexProducts(batchIds);
            total += batchIds.size();
            if (batchIds.size() < REINDEX_PAGE_SIZE) {
                break;
            }
            pageNum++;
        }

        log.info("Reindexed {} products to ES", total);
        return total;
    }
}
