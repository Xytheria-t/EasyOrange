package com.cartethyia.easyorange.adapter.outbound.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Admin 产品查询适配器
 * 实现 AdminProductQueryPort，通过 Product Mapper 查询数据并转换为 Admin 模块需要的格式
 */
@Primary
@Component
@RequiredArgsConstructor
public class AdminProductQueryAdapter implements AdminProductQueryPort {

    private final ProductMapper productMapper;
    private final ProductDetailMapper productDetailMapper;
    private final ProductImageMapper productImageMapper;

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
    public AiReviewData getAiReviewData(String productId) {
        ProductDO product = productMapper.selectById(productId);
        if (product == null || product.getDelFlag() != 0) {
            return null;
        }

        List<ProductDetailDO> details = productMapper.selectDetailsByProductIds(List.of(productId));
        String description = details.isEmpty() ? null : details.get(0).getDescription();

        List<CategoryDO> categories = productMapper.selectCategoriesByIds(List.of(product.getCategoryId()));
        String categoryName = categories.isEmpty() ? null : categories.get(0).getName();

        List<SellerReadModel> sellers = productMapper.selectSellersByIds(Set.of(product.getUserId()));
        String sellerName = sellers.isEmpty() ? null : sellers.get(0).nickName();

        List<String> imageUrls = productMapper.selectImagesByProductIds(List.of(productId)).stream()
                .map(ProductImageDO::getImageUrl)
                .toList();

        return new AiReviewData(
                product.getName(),
                description,
                categoryName,
                product.getConditionLevel() != null
                        ? product.getConditionLevel().getCode()
                        : null,
                product.getPrice(),
                sellerName,
                imageUrls);
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
