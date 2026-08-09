package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
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

    private final ProductMapper productMapper;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchIndexManager indexManager;
    private final ElasticsearchProductSearchIndexAdapter indexAdapter;

    /**
     * 全量重建索引：删除旧索引 → 按 JSON 配置重建索引 → 批量写入在线商品。
     *
     * <p>删除后必须先按 {@code product-mapping.json}/{@code product-settings.json} 重建索引：
     * 否则 {@code ElasticsearchOperations.save} 会触发 ES 动态 auto-create 出错误的动态 mapping
     * （丢失 IK analyzer、dense_vector 等字段定义），后续语义/分析全部失效。</p>
     *
     * <p>写入走 {@link ElasticsearchProductSearchIndexAdapter#indexProducts} 的批量预加载路径，
     * 避免逐商品 N+1（每个商品 4 次关联查询）。</p>
     */
    public int reindexAll() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexManager.createProductIndex();

        List<String> productIds = ChainWrappers.lambdaQueryChain(productMapper)
                .eq(ProductDO::getStatus, ProductStatus.ONLINE.getCode())
                .list()
                .stream()
                .map(ProductDO::getId)
                .toList();

        indexAdapter.indexProducts(productIds);

        log.info("Reindexed {} products to ES", productIds.size());
        return productIds.size();
    }
}
