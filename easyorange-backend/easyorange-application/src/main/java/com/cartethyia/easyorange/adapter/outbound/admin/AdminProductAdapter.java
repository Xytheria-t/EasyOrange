package com.cartethyia.easyorange.adapter.outbound.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Admin 商品查询/操作适配器
 * <p>
 * 实现 {@link AdminProductPort}，通过 Product Mapper / Repository 访问商品数据并转换为 Admin 模块需要的格式。
 * 审核/举报/分类/仪表板功能域见同包下各自的 Adapter。
 */
@Primary
@Component
@RequiredArgsConstructor
public class AdminProductAdapter implements AdminProductPort {

    private final ProductMapper productMapper;
    private final ProductDetailMapper productDetailMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductRepository productRepository;
    private final ProductCacheEvictionPort productCacheEvictionPort;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public ProductQueryResult queryProducts(ProductQueryCondition condition) {
        var wrapper = ChainWrappers.lambdaQueryChain(productMapper).eq(ProductDO::getDelFlag, 0);

        if (condition.keyword() != null && !condition.keyword().isEmpty()) {
            wrapper.like(ProductDO::getName, condition.keyword());
        }
        if (condition.categoryId() != null) {
            wrapper.eq(ProductDO::getCategoryId, condition.categoryId());
        }
        if (condition.status() != null) {
            wrapper.eq(ProductDO::getStatus, condition.status());
        }
        if (condition.sellerId() != null) {
            wrapper.eq(ProductDO::getUserId, condition.sellerId());
        }
        if (condition.startTime() != null) {
            wrapper.ge(ProductDO::getCreateTime, condition.startTime());
        }
        if (condition.endTime() != null) {
            wrapper.le(ProductDO::getCreateTime, condition.endTime());
        }

        wrapper.orderByDesc(ProductDO::getCreateTime);

        int pageNum = condition.pageNum() != null ? condition.pageNum() : 1;
        int pageSize = condition.pageSize() != null ? condition.pageSize() : 20;
        Page<ProductDO> page = wrapper.page(new Page<>(pageNum, pageSize));

        List<ProductSummary> records =
                page.getRecords().stream().map(this::toProductSummary).collect(Collectors.toList());

        return new ProductQueryResult(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public ProductDetail getProductDetail(String productId) {
        ProductDO product = productMapper.selectById(productId);
        if (product == null || product.getDelFlag() != 0) {
            return null;
        }

        List<ProductDetailDO> details = productDetailMapper.selectDetailsByProductIds(List.of(productId));
        String description = details.isEmpty() ? null : details.get(0).getDescription();

        return new ProductDetail(
                product.getId(),
                product.getName(),
                description,
                product.getPrice(),
                product.getOriginalPrice(),
                product.getStock(),
                product.getStatus() != null ? product.getStatus().getCode() : null,
                product.getStatus() != null ? product.getStatus().getDesc() : null,
                product.getConditionLevel() != null
                        ? product.getConditionLevel().getCode()
                        : null,
                product.getLocation(),
                product.getContactMethod(),
                product.getCategoryId(),
                product.getUserId(),
                product.getViewCount(),
                product.getCreateTime(),
                product.getUpdateTime());
    }

    @Override
    public Map<String, List<String>> getProductImages(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductImageDO> images = ChainWrappers.lambdaQueryChain(productImageMapper)
                .in(ProductImageDO::getProductId, productIds)
                .orderByAsc(ProductImageDO::getSortOrder)
                .list();
        return images.stream()
                .collect(Collectors.groupingBy(
                        ProductImageDO::getProductId,
                        Collectors.mapping(ProductImageDO::getImageUrl, Collectors.toList())));
    }

    @Override
    public Map<String, ProductInfo> getProductInfos(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductDO> products = productMapper.selectByIds(productIds);
        return products.stream()
                .filter(p -> p.getDelFlag() == 0)
                .collect(Collectors.toMap(ProductDO::getId, p -> new ProductInfo(p.getId(), p.getName()), (a, b) -> a));
    }

    @Override
    public void applyProductStatus(String productId, String statusCode) {
        ProductStatus newStatus;
        try {
            newStatus = ProductStatus.fromCode(statusCode);
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of("无效的商品状态");
        }

        Product product = findProductOrThrow(productId);

        Transition<Product, ?> transition =
                switch (newStatus) {
                    case ONLINE -> product.putOnline();
                    case OFFLINE -> product.takeOffline();
                    case SOLD -> product.markAsSold().orElse(null);
                    default -> throw BusinessException.of("不支持将该商品状态改为: " + newStatus.getDesc());
                };
        if (transition != null) {
            productRepository.save(transition.aggregate());
            domainEventPublisher.publish(transition.event());
        }
        productCacheEvictionPort.evictProductCache(productId);
    }

    private Product findProductOrThrow(String productId) {
        return productRepository.findById(ProductId.of(productId)).orElseThrow(() -> BusinessException.of("商品不存在"));
    }

    private ProductSummary toProductSummary(ProductDO product) {
        return new ProductSummary(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getOriginalPrice(),
                product.getStock(),
                product.getStatus() != null ? product.getStatus().getCode() : null,
                product.getStatus() != null ? product.getStatus().getDesc() : null,
                product.getConditionLevel() != null
                        ? product.getConditionLevel().getCode()
                        : null,
                product.getLocation(),
                product.getContactMethod(),
                product.getCategoryId(),
                product.getUserId(),
                product.getViewCount(),
                product.getCreateTime(),
                product.getUpdateTime());
    }
}
