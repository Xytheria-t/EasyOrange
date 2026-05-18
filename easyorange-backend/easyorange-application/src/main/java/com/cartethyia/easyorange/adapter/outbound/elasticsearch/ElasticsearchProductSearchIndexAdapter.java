package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.CategoryMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductImageMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.port.output.ProductSearchIndexPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
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

    @Override
    public void indexProduct(Long productId) {
        saveDocument(productId);
    }

    @Override
    public void updateProductIndex(Long productId) {
        saveDocument(productId);
    }

    @Override
    public void removeProductIndex(Long productId) {
        elasticsearchOperations.delete(String.valueOf(productId), ProductDocument.class);
        log.debug("Deleted ES document for productId={}", productId);
    }

    private void saveDocument(Long productId) {
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

    /** 将 ProductDO 组装为 ES ProductDocument（含关联查询） */
    ProductDocument buildDocument(ProductDO product) {
        Long productId = product.getId();

        // 查询描述
        ProductDetailDO detail = productDetailMapper.selectById(productId);

        // 查询图片（取主图 URL，取第一张作为主图）
        List<ProductImageDO> imageList = ChainWrappers.lambdaQueryChain(productImageMapper)
                .eq(ProductImageDO::getProductId, productId)
                .orderByAsc(ProductImageDO::getSortOrder)
                .list();
        String mainImage = null;
        List<String> imageUrls = List.of();
        if (!imageList.isEmpty()) {
            // 取 isMain=1 的图片，或第一张
            mainImage = imageList.stream()
                    .filter(img -> img.getIsMain() != null && img.getIsMain() == 1)
                    .findFirst()
                    .map(ProductImageDO::getImageUrl)
                    .orElse(imageList.get(0).getImageUrl());
            imageUrls = imageList.stream()
                    .map(ProductImageDO::getImageUrl)
                    .collect(Collectors.toList());
        }

        // 查询分类名称
        String categoryName = null;
        if (product.getCategoryId() != null) {
            CategoryDO category = categoryMapper.selectById(product.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }

        // 标签转 List
        List<String> tagList = product.getTags() != null && !product.getTags().isBlank()
                ? Arrays.stream(product.getTags().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList())
                : List.of();

        return ProductDocument.builder()
                .id(String.valueOf(productId))
                .userId(product.getUserId())
                .name(product.getName())
                .description(detail != null ? detail.getDescription() : null)
                .categoryId(product.getCategoryId() != null ? product.getCategoryId().intValue() : null)
                .categoryName(categoryName)
                .price(product.getPrice() != null ? product.getPrice().doubleValue() : null)
                .originalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice().doubleValue() : null)
                .conditionLevel(product.getConditionLevel() != null ? product.getConditionLevel().byteValue() : null)
                .status(product.getStatus() != null ? product.getStatus().byteValue() : null)
                .viewCount(product.getViewCount())
                .stock(product.getStock())
                .location(product.getLocation())
                .tags(tagList)
                .mainImage(mainImage)
                .images(imageUrls)
                .createTime(product.getCreateTime())
                .updateTime(product.getUpdateTime())
                .build();
    }
}
