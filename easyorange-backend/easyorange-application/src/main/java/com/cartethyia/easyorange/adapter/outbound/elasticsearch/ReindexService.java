package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

        // 读取所有在线商品（ONLINE status = 1）
        List<ProductDO> products = productMapper.selectList(
                new LambdaQueryWrapper<ProductDO>()
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
