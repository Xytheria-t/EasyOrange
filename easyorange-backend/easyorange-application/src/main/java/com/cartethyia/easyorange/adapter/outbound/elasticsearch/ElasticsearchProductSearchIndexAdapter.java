package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.port.ProductSearchIndexPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final ProductImageMapper productImageMapper;
    private final CategoryMapper categoryMapper;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    @Override
    public void indexProduct(String productId) {
        saveDocument(productId);
    }

    @Override
    public void updateProductIndex(String productId) {
        saveDocument(productId);
    }

    @Override
    public void removeProductIndex(String productId) {
        elasticsearchOperations.delete(productId, ProductDocument.class);
        log.debug("Deleted ES document for productId={}", productId);
    }

    /**
     * 保存单个商品到 ES 索引。
     *
     * <p><b>性能说明</b></p>
     * <p>此方法会执行 4 次数据库查询：</p>
     * <ul>
     *   <li>1 次查询商品基本信息</li>
     *   <li>1 次查询商品详情</li>
     *   <li>1 次查询商品图片列表</li>
     *   <li>1 次查询分类信息</li>
     * </ul>
     * <p>对于单个商品索引操作，这个查询开销是可接受的。</p>
     * <p><b>批量操作请使用 {@link #indexProducts(List)} 方法，避免 N+1 查询问题。</b></p>
     *
     * @param productId 商品 ID
     */
    private void saveDocument(String productId) {
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

    /**
     * 将 ProductDO 组装为 ES ProductDocument（含关联查询）。
     *
     * <p><b>警告：N+1 查询风险</b></p>
     * <p>此方法会执行 3 次数据库查询：ProductDetail、ProductImage、Category。</p>
     * <p>如果循环调用此方法处理多个商品，会产生 3N 次查询。</p>
     * <p><b>批量操作请使用 {@link #indexProducts(List)} 方法，它会批量预加载所有关联数据。</b></p>
     *
     * @param product 商品 DO
     * @return ES 文档对象
     */
    ProductDocument buildDocument(ProductDO product) {
        String productId = product.getId();

        ProductDetailDO detail = productDetailMapper.selectById(productId);

        List<ProductImageDO> imageList = ChainWrappers.lambdaQueryChain(productImageMapper)
                .eq(ProductImageDO::getProductId, productId)
                .orderByAsc(ProductImageDO::getSortOrder)
                .list();

        String categoryName = null;
        if (product.getCategoryId() != null) {
            CategoryDO category = categoryMapper.selectById(product.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }

        return buildDocument(product, detail, imageList, categoryName);
    }

    /** 使用预加载的数据构建文档（批量操作使用，消除 N+1 查询） */
    private ProductDocument buildDocument(ProductDO product,
                                          Map<String, ProductDetailDO> detailMap,
                                          Map<String, List<ProductImageDO>> imagesByProduct,
                                          Map<String, CategoryDO> categoryMap) {
        String productId = product.getId();

        ProductDetailDO detail = detailMap.get(productId);
        List<ProductImageDO> imageList = imagesByProduct.getOrDefault(productId, List.of());

        String categoryName = null;
        if (product.getCategoryId() != null) {
            CategoryDO category = categoryMap.get(product.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }

        return buildDocument(product, detail, imageList, categoryName);
    }

    /** 核心文档构建逻辑（无数据库查询） */
    private ProductDocument buildDocument(ProductDO product,
                                           ProductDetailDO detail,
                                           List<ProductImageDO> imageList,
                                           String categoryName) {
        String productId = product.getId();

        String mainImage = null;
        List<String> imageUrls = List.of();
        if (!imageList.isEmpty()) {
            mainImage = imageList.stream()
                    .filter(img -> img.getIsMain() != null && img.getIsMain() == 1)
                    .findFirst()
                    .map(ProductImageDO::getImageUrl)
                    .orElse(imageList.get(0).getImageUrl());
            imageUrls = imageList.stream()
                    .map(ProductImageDO::getImageUrl)
                    .collect(Collectors.toList());
        }

        List<String> tagList = product.getTags() != null && !product.getTags().isBlank()
                ? Arrays.stream(product.getTags().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList())
                : List.of();

        return ProductDocument.builder()
                .id(productId)
                .userId(product.getUserId())
                .name(product.getName())
                .description(detail != null ? detail.getDescription() : null)
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
                .price(product.getPrice() != null ? product.getPrice().doubleValue() : null)
                .originalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice().doubleValue() : null)
                .conditionLevel(product.getConditionLevel() != null ? product.getConditionLevel().getCode() : null)
                .status(product.getStatus() != null ? product.getStatus().getCode() : null)
                .viewCount(product.getViewCount())
                .stock(product.getStock())
                .location(product.getLocation())
                .tags(tagList)
                .mainImage(mainImage)
                .images(imageUrls)
                .nameEmbedding(embedName(product.getName()))
                .createTime(product.getCreateTime())
                .updateTime(product.getUpdateTime())
                .build();
    }

    /**
     * 商品名向量化（best-effort）：用于 ES kNN 语义搜索。
     * <p>embedding 服务不可用或调用失败时返回 {@code null}，索引照常写入（仅缺失向量匹配能力，不阻塞索引）。</p>
     */
    private List<Float> embedName(String name) {
        EmbeddingModel model = embeddingModelProvider.getIfAvailable();
        if (model == null || name == null || name.isBlank()) {
            return null;
        }
        try {
            float[] arr = model.embed(name);
            var embedding = new ArrayList<Float>(arr.length);
            for (float value : arr) {
                embedding.add(value);
            }
            return embedding;
        } catch (Exception e) {
            log.warn("Failed to embed product name for ES index: {}", name, e);
            return null;
        }
    }

    /**
     * 批量索引商品到 ES。
     * 先批量加载所有关联数据到内存 Map，再逐条构建 document 并批量保存，消除 N+1 查询问题。
     */
    public void indexProducts(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        try {
            List<ProductDocument> docs = loadDocumentsBulk(productIds);
            if (!docs.isEmpty()) {
                elasticsearchOperations.save(docs);
            }
            log.debug("Batch indexed {} products to ES", docs.size());
        } catch (Exception e) {
            log.error("Failed to batch index products to ES", e);
        }
    }

    /** 批量加载所有关联数据到内存 Map，再逐条构建 document */
    private List<ProductDocument> loadDocumentsBulk(List<String> productIds) {
        List<ProductDO> products = productMapper.selectByIds(productIds);
        if (products.isEmpty()) {
            return List.of();
        }

        Map<String, ProductDetailDO> detailMap = productDetailMapper
                .selectDetailsByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(ProductDetailDO::getProductId, d -> d, (a, b) -> a));

        Map<String, List<ProductImageDO>> imagesByProduct = ChainWrappers.lambdaQueryChain(productImageMapper)
                .in(ProductImageDO::getProductId, productIds)
                .orderByAsc(ProductImageDO::getSortOrder)
                .list()
                .stream()
                .collect(Collectors.groupingBy(ProductImageDO::getProductId));

        Set<String> categoryIds = products.stream()
                .map(ProductDO::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, CategoryDO> categoryMap = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectByIds(categoryIds)
                        .stream()
                        .collect(Collectors.toMap(CategoryDO::getId, c -> c, (a, b) -> a));

        return products.stream()
                .map(product -> buildDocument(product, detailMap, imagesByProduct, categoryMap))
                .collect(Collectors.toList());
    }
}