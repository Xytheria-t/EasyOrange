package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.util.BatchQueryUtil;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.admin.dto.request.AdminProductQueryRequest;
import com.cartethyia.easyorange.admin.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.dto.response.AdminProductResponse;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductImageMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductMapper productMapper;
    private final ProductDetailMapper productDetailMapper;
    private final ProductImageMapper productImageMapper;

    public PageResult<AdminProductResponse> listProducts(AdminProductQueryRequest request) {
        int pageNum = request.pageNum() != null ? request.pageNum() : 1;
        int pageSize = request.pageSize() != null ? request.pageSize() : 20;

        var wrapper = ChainWrappers.lambdaQueryChain(productMapper)
            .eq(ProductDO::getDelFlag, 0);

        if (StringUtils.hasText(request.keyword())) {
            wrapper.like(ProductDO::getName, request.keyword());
        }

        if (request.categoryId() != null) {
            wrapper.eq(ProductDO::getCategoryId, request.categoryId());
        }

        if (request.status() != null) {
            wrapper.eq(ProductDO::getStatus, request.status());
        }

        if (request.sellerId() != null) {
            wrapper.eq(ProductDO::getUserId, request.sellerId());
        }

        if (StringUtils.hasText(request.startTime())) {
            try {
                LocalDateTime startTime = LocalDateTime.parse(request.startTime() + " 00:00:00", BatchQueryUtil.DATE_FORMATTER);
                wrapper.ge(ProductDO::getCreateTime, startTime);
            } catch (Exception ignored) {
            }
        }

        if (StringUtils.hasText(request.endTime())) {
            try {
                LocalDateTime endTime = LocalDateTime.parse(request.endTime() + " 23:59:59", BatchQueryUtil.DATE_FORMATTER);
                wrapper.le(ProductDO::getCreateTime, endTime);
            } catch (Exception ignored) {
            }
        }

        wrapper.orderByDesc(ProductDO::getCreateTime);

        Page<ProductDO> page = wrapper.page(new Page<>(pageNum, pageSize));

        List<Long> productIds = page.getRecords().stream()
            .map(ProductDO::getId)
            .collect(Collectors.toList());

        Map<Long, List<String>> imagesMap = getImagesMap(productIds);

        List<AdminProductResponse> records = page.getRecords().stream()
            .map(p -> toAdminProductResponse(p, imagesMap))
            .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public AdminProductResponse getProductDetail(Long id) {
        ProductDO product = productMapper.selectById(id);
        if (product == null || product.getDelFlag() != 0) {
            throw BusinessException.of("商品不存在");
        }

        Map<Long, List<String>> imagesMap = getImagesMap(List.of(id));

        List<ProductDetailDO> details = productDetailMapper.selectDetailsByProductIds(List.of(id));
        String description = details.isEmpty() ? null : details.get(0).getDescription();

        AdminProductResponse vo = toAdminProductResponse(product, imagesMap);
        return AdminProductResponse.builder()
            .productId(vo.productId())
            .name(vo.name())
            .description(description)
            .price(vo.price())
            .originalPrice(vo.originalPrice())
            .stock(vo.stock())
            .status(vo.status())
            .statusDesc(vo.statusDesc())
            .conditionLevel(vo.conditionLevel())
            .location(vo.location())
            .contactMethod(vo.contactMethod())
            .images(vo.images())
            .mainImage(vo.mainImage())
            .categoryId(vo.categoryId())
            .categoryName(vo.categoryName())
            .sellerId(vo.sellerId())
            .sellerName(vo.sellerName())
            .sellerAvatar(vo.sellerAvatar())
            .viewCount(vo.viewCount())
            .createTime(vo.createTime())
            .updateTime(vo.updateTime())
            .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProductStatus(Long id, UpdateStatusRequest request) {
        ProductDO product = productMapper.selectById(id);
        if (product == null || product.getDelFlag() != 0) {
            throw BusinessException.of("商品不存在");
        }

        ProductStatus newStatus = ProductStatus.fromCode(request.getStatus());
        if (newStatus == null) {
            throw BusinessException.of("无效的商品状态");
        }

        product.setStatus(newStatus.getCode());
        productMapper.updateById(product);
    }

    private Map<Long, List<String>> getImagesMap(List<Long> productIds) {
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
                Collectors.mapping(ProductImageDO::getImageUrl, Collectors.toList())
            ));
    }

    private AdminProductResponse toAdminProductResponse(ProductDO product, Map<Long, List<String>> imagesMap) {
        List<String> images = imagesMap.getOrDefault(product.getId(), List.of());
        String mainImage = images.isEmpty() ? null : images.get(0);

        return AdminProductResponse.builder()
            .productId(product.getId())
            .name(product.getName())
            .price(product.getPrice())
            .originalPrice(product.getOriginalPrice())
            .stock(product.getStock())
            .status(product.getStatus())
            .statusDesc(ProductStatus.getDescByCode(product.getStatus()))
            .conditionLevel(product.getConditionLevel())
            .location(product.getLocation())
            .contactMethod(product.getContactMethod())
            .images(images)
            .mainImage(mainImage)
            .categoryId(product.getCategoryId())
            .sellerId(product.getUserId())
            .viewCount(product.getViewCount())
            .createTime(product.getCreateTime())
            .updateTime(product.getUpdateTime())
            .build();
    }
}
