package com.cartethyia.easyorange.controller.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.controller.admin.dto.request.AdminProductQueryRequest;
import com.cartethyia.easyorange.controller.admin.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.controller.admin.dto.response.AdminProductVO;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductMapper productMapper;
    private final ProductDetailMapper productDetailMapper;
    private final ProductImageMapper productImageMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PageResult<AdminProductVO> listProducts(AdminProductQueryRequest request) {
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        LambdaQueryWrapper<ProductDO> wrapper = new LambdaQueryWrapper<ProductDO>()
            .eq(ProductDO::getDelFlag, 0);

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(ProductDO::getName, request.getKeyword());
        }

        if (request.getCategoryId() != null) {
            wrapper.eq(ProductDO::getCategoryId, request.getCategoryId());
        }

        if (request.getStatus() != null) {
            wrapper.eq(ProductDO::getStatus, request.getStatus());
        }

        if (request.getSellerId() != null) {
            wrapper.eq(ProductDO::getUserId, request.getSellerId());
        }

        if (StringUtils.hasText(request.getStartTime())) {
            try {
                LocalDateTime startTime = LocalDateTime.parse(request.getStartTime() + " 00:00:00", DATE_FORMATTER);
                wrapper.ge(ProductDO::getCreateTime, startTime);
            } catch (Exception ignored) {
            }
        }

        if (StringUtils.hasText(request.getEndTime())) {
            try {
                LocalDateTime endTime = LocalDateTime.parse(request.getEndTime() + " 23:59:59", DATE_FORMATTER);
                wrapper.le(ProductDO::getCreateTime, endTime);
            } catch (Exception ignored) {
            }
        }

        wrapper.orderByDesc(ProductDO::getCreateTime);

        Page<ProductDO> page = productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<Long> productIds = page.getRecords().stream()
            .map(ProductDO::getId)
            .collect(Collectors.toList());

        Map<Long, List<String>> imagesMap = getImagesMap(productIds);

        List<AdminProductVO> records = page.getRecords().stream()
            .map(p -> toAdminProductVO(p, imagesMap))
            .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    public AdminProductVO getProductDetail(Long id) {
        ProductDO product = productMapper.selectById(id);
        if (product == null || product.getDelFlag() != 0) {
            throw BusinessException.of("商品不存在");
        }

        Map<Long, List<String>> imagesMap = getImagesMap(List.of(id));

        List<ProductDetailDO> details = productDetailMapper.selectDetailsByProductIds(List.of(id));
        String description = details.isEmpty() ? null : details.get(0).getDescription();

        AdminProductVO vo = toAdminProductVO(product, imagesMap);
        return AdminProductVO.builder()
            .productId(vo.getProductId())
            .name(vo.getName())
            .description(description)
            .price(vo.getPrice())
            .originalPrice(vo.getOriginalPrice())
            .stock(vo.getStock())
            .status(vo.getStatus())
            .statusDesc(vo.getStatusDesc())
            .conditionLevel(vo.getConditionLevel())
            .location(vo.getLocation())
            .contactMethod(vo.getContactMethod())
            .images(vo.getImages())
            .mainImage(vo.getMainImage())
            .categoryId(vo.getCategoryId())
            .categoryName(vo.getCategoryName())
            .sellerId(vo.getSellerId())
            .sellerName(vo.getSellerName())
            .sellerAvatar(vo.getSellerAvatar())
            .viewCount(vo.getViewCount())
            .createTime(vo.getCreateTime())
            .updateTime(vo.getUpdateTime())
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
        List<ProductImageDO> images = productImageMapper.selectList(
            new LambdaQueryWrapper<ProductImageDO>()
                .in(ProductImageDO::getProductId, productIds)
                .orderByAsc(ProductImageDO::getSortOrder)
        );
        return images.stream()
            .collect(Collectors.groupingBy(
                ProductImageDO::getProductId,
                Collectors.mapping(ProductImageDO::getImageUrl, Collectors.toList())
            ));
    }

    private AdminProductVO toAdminProductVO(ProductDO product, Map<Long, List<String>> imagesMap) {
        List<String> images = imagesMap.getOrDefault(product.getId(), List.of());
        String mainImage = images.isEmpty() ? null : images.get(0);

        return AdminProductVO.builder()
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
