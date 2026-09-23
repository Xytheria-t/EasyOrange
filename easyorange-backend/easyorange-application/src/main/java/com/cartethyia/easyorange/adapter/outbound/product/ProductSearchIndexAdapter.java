package com.cartethyia.easyorange.adapter.outbound.product;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.domain.port.ProductSearchIndexPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MySQL search_text 维护适配器 — 始终激活（不随 ES 开关条件装配）。
 *
 * <p>把商品数据同步到 {@code eo_product.search_text} 列（ngram FULLTEXT {@code ft_eo_product_search_text}），
 * 覆盖商品名称、描述、位置和标签等更丰富的字段，供 ES 关闭时 {@code searchByFullText} 降级检索。</p>
 *
 * <p>ES 开启时端口注入的是 {@code ElasticsearchProductSearchIndexAdapter}（{@code @Primary}），
 * 它在写 ES 的同时委托本适配器双写 search_text —— 保证任意时刻关掉 ES，MySQL 侧语料都是热的。</p>
 */
@Slf4j
@Component
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
        try {
            productMapper.updateSearchText(productId, null);
            log.debug("Cleared search_text for productId={}", productId);
        } catch (Exception e) {
            log.error("Failed to clear search_text for productId={}", productId, e);
        }
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
