package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.entity.ProductDetail;
import com.cartethyia.easyorange.product.entity.ProductImage;
import com.cartethyia.easyorange.product.mapper.ProductDetailMapper;
import com.cartethyia.easyorange.product.mapper.ProductImageMapper;
import com.cartethyia.easyorange.product.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductAttachmentRepositoryImpl implements ProductAttachmentRepository {

    private final ProductDetailMapper productDetailMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductImageService productImageService;

    @Override
    public ProductDetail findDetailByProductId(Long productId) {
        return productDetailMapper.selectById(productId);
    }

    @Override
    @Transactional
    public void saveDetail(ProductDetail detail) {
        productDetailMapper.insert(detail);
    }

    @Override
    @Transactional
    public void updateDetail(ProductDetail detail) {
        productDetailMapper.updateById(detail);
    }

    @Override
    @Transactional
    public void deleteDetailByProductId(Long productId) {
        productDetailMapper.deleteById(productId);
    }

    @Override
    public List<ProductImage> findImagesByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return productImageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductImage>()
                        .in(ProductImage::getProductId, productIds)
                        .orderByAsc(ProductImage::getSortOrder)
        );
    }

    @Override
    @Transactional
    public void saveImages(Long productId, List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        for (ProductImage image : images) {
            image.setProductId(productId);
        }
        productImageService.saveBatch(images);
    }

    @Override
    @Transactional
    public void deleteImagesByProductId(Long productId) {
        productImageService.deleteByProductId(productId);
    }
}
