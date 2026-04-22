package com.cartethyia.easyorange.product.service.impl;

import com.cartethyia.easyorange.product.entity.ProductDetail;
import com.cartethyia.easyorange.product.entity.ProductImage;
import com.cartethyia.easyorange.product.mapper.ProductDetailMapper;
import com.cartethyia.easyorange.product.service.ProductAttachmentService;
import com.cartethyia.easyorange.product.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductAttachmentServiceImpl implements ProductAttachmentService {

    private final ProductDetailMapper productDetailMapper;
    private final ProductImageService productImageService;

    @Override
    public ProductDetail getDetailByProductId(Long productId) {
        return productDetailMapper.selectById(productId);
    }

    @Override
    public void saveDetail(ProductDetail detail) {
        productDetailMapper.insert(detail);
    }

    @Override
    public void updateDetail(ProductDetail detail) {
        productDetailMapper.updateById(detail);
    }

    @Override
    public void deleteDetailByProductId(Long productId) {
        productDetailMapper.deleteById(productId);
    }

    @Override
    public List<ProductImage> listImagesByProductIds(List<Long> productIds) {
        return productImageService.listByProductIds(productIds);
    }

    @Override
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
    public void deleteImagesByProductId(Long productId) {
        productImageService.deleteByProductId(productId);
    }

    @Override
    public AggregateData loadAggregateData(Long productId) {
        ProductDetail detail = getDetailByProductId(productId);
        List<ProductImage> images = listImagesByProductIds(List.of(productId));
        return new AggregateData(detail, images);
    }
}