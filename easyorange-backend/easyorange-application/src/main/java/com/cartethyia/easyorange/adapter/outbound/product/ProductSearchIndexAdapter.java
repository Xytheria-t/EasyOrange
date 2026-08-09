package com.cartethyia.easyorange.adapter.outbound.product;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.domain.port.ProductSearchIndexPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 商品搜索索引适配器
 *
 * <p>将商品数据同步到 MySQL 的 search_text 列（已建 ngram FULLTEXT 索引），
 * 使搜索能覆盖商品名称、描述、位置和标签等更丰富的字段。
 * 当未来迁移至 Elasticsearch 等专用搜索引擎时，只需替换此适配器实现。</p>
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class ProductSearchIndexAdapter implements ProductSearchIndexPort {

    private final ProductMapper productMapper;
    private final ProductDetailMapper productDetailMapper;

    @Override
    public void indexProduct(String productId) {
        updateSearchText(productId);
    }

    @Override
    public void updateProductIndex(String productId) {
        updateSearchText(productId);
    }

    @Override
    public void removeProductIndex(String productId) {
        productMapper.updateSearchText(productId, null);
        log.debug("Cleared search_text for productId={}", productId);
    }

    private void updateSearchText(String productId) {
        try {
            ProductDO product = productMapper.selectById(productId);
            if (product == null) {
                log.warn("Product not found for search index update, productId={}", productId);
                return;
            }

            ProductDetailDO detail = productDetailMapper.selectById(productId);
            String searchText = buildSearchText(product, detail);

            productMapper.updateSearchText(productId, searchText);
            log.debug("Updated search_text for productId={}, length={}", productId, searchText.length());
        } catch (Exception e) {
            log.error("Failed to update search index for productId={}", productId, e);
        }
    }

    /**
     * 将商品的多个可搜索字段拼接为 search_text。
     * 此字段由 MySQL ngram FULLTEXT 索引分词，使商品搜索支持名称、描述、位置、标签等多维度匹配。
     */
    static String buildSearchText(ProductDO product, ProductDetailDO detail) {
        var sb = new StringBuilder();

        if (product.getName() != null) {
            sb.append(product.getName()).append(' ');
        }

        if (detail != null
                && detail.getDescription() != null
                && !detail.getDescription().isBlank()) {
            sb.append(detail.getDescription()).append(' ');
        }

        if (product.getLocation() != null && !product.getLocation().isBlank()) {
            sb.append(product.getLocation()).append(' ');
        }

        if (product.getTags() != null && !product.getTags().isBlank()) {
            sb.append(product.getTags().replace(',', ' ')).append(' ');
        }

        return sb.toString().trim();
    }
}
