package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.adapter.outbound.product.ProductSearchIndexAdapter;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.application.port.query.QueryEmbeddingPort;
import com.cartethyia.easyorange.product.domain.port.ProductSearchIndexPort;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

/**
 * ES 实现的商品搜索索引适配器 — 当 easyorange.search.elasticsearch.enabled=true 时激活并接管端口（{@code @Primary}）；
 * 写 ES 的同时委托 {@link com.cartethyia.easyorange.adapter.outbound.product.ProductSearchIndexAdapter}
 * 双写 MySQL search_text —— 降级语料始终是热的，任意时刻关 ES 检索照常可用。
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ElasticsearchProductSearchIndexAdapter implements ProductSearchIndexPort {

    private final ProductMapper productMapper;
    private final ProductDetailMapper productDetailMapper;
    private final ProductImageMapper productImageMapper;
    private final CategoryMapper categoryMapper;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectProvider<QueryEmbeddingPort> queryEmbeddingPort;
    private final ProductSearchIndexAdapter mysqlSearchTextIndex;

    @Override
    public void indexProduct(String productId) {
        mysqlSearchTextIndex.indexProduct(productId);
        saveDocument(productId);
    }

    @Override
    public void updateProductIndex(String productId) {
        mysqlSearchTextIndex.updateProductIndex(productId);
        saveDocument(productId);
    }

    @Override
    public void removeProductIndex(String productId) {
        mysqlSearchTextIndex.removeProductIndex(productId);
        elasticsearchOperations.delete(productId, ProductDocument.class);
        log.debug("Deleted ES document for productId={}", productId);
    }

    /** 写 ES 失败只记 error 不外抛：索引只是检索旁路，写侧事务已落 MySQL，抛出去会把商品发布打成失败。 */
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

    /** 单条构建走 3 次关联查询（详情/图片/分类），只用于单商品路径；批量请走 {@link #indexProducts(List)} 免 N+1。 */
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

    private ProductDocument buildDocument(
            ProductDO product,
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

    /** 纯组装不查库：单条与批量两条路径的公共终点。 */
    private ProductDocument buildDocument(
            ProductDO product, ProductDetailDO detail, List<ProductImageDO> imageList, String categoryName) {
        String productId = product.getId();

        String mainImage = null;
        List<String> imageUrls = List.of();
        if (!imageList.isEmpty()) {
            mainImage = imageList.stream()
                    .filter(img -> img.getIsMain() != null && img.getIsMain() == 1)
                    .findFirst()
                    .map(ProductImageDO::getImageUrl)
                    .orElse(imageList.get(0).getImageUrl());
            imageUrls = imageList.stream().map(ProductImageDO::getImageUrl).collect(Collectors.toList());
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
                .originalPrice(
                        product.getOriginalPrice() != null
                                ? product.getOriginalPrice().doubleValue()
                                : null)
                .conditionLevel(
                        product.getConditionLevel() != null
                                ? product.getConditionLevel().getCode()
                                : null)
                .status(product.getStatus() != null ? product.getStatus().getCode() : null)
                .viewCount(product.getViewCount())
                .stock(product.getStock())
                .location(product.getLocation())
                .tags(tagList)
                .mainImage(mainImage)
                .images(imageUrls)
                .nameEmbedding(embedName(product.getName()))
                .createTime(toEpochMillis(product.getCreateTime()))
                .updateTime(toEpochMillis(product.getUpdateTime()))
                .build();
    }

    /**
     * LocalDateTime → epoch 毫秒：ES 文档时间以 Long（epoch_millis）存储，规避 Spring Data ES 6 对
     * LocalDateTime 的默认序列化陷阱；zone 用系统默认时区——与库里 DATETIME 同为本地墙钟语义。
     */
    private static Long toEpochMillis(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 商品名向量化（best-effort）：走 {@link QueryEmbeddingPort} 而不是直接调 {@code EmbeddingModel} —— 查询侧与
     * 索引侧必须同一个编码器（同模型同维度，否则 kNN 相似度没有意义），顺带同享预算记账与调用日志。
     * 端口缺失、返回空或抛异常（日预算耗尽）一律返回 {@code null}：仅缺失向量匹配能力，不阻塞索引。
     */
    private List<Float> embedName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        var port = queryEmbeddingPort.getIfAvailable();
        if (port == null) {
            return null;
        }
        try {
            List<Float> vector = port.embed(name);
            return vector.isEmpty() ? null : vector;
        } catch (Exception e) {
            log.warn("Failed to embed product name for ES index: {}", name, e);
            return null;
        }
    }

    /**
     * 批量索引商品到 ES — 先批量加载所有关联数据到内存 Map，再逐条构建 document 并批量保存，消除 N+1 查询问题。
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

    private List<ProductDocument> loadDocumentsBulk(List<String> productIds) {
        List<ProductDO> products = productMapper.selectByIds(productIds);
        if (products.isEmpty()) {
            return List.of();
        }

        Map<String, ProductDetailDO> detailMap = productDetailMapper.selectDetailsByProductIds(productIds).stream()
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
        Map<String, CategoryDO> categoryMap = categoryIds.isEmpty()
                ? Map.of()
                : categoryMapper.selectByIds(categoryIds).stream()
                        .collect(Collectors.toMap(CategoryDO::getId, c -> c, (a, b) -> a));

        return products.stream()
                .map(product -> buildDocument(product, detailMap, imagesByProduct, categoryMap))
                .collect(Collectors.toList());
    }
}
