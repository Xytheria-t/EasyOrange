package com.cartethyia.easyorange.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.product.constant.ProductConstants;
import com.cartethyia.easyorange.product.entity.ProductImage;
import com.cartethyia.easyorange.product.mapper.ProductImageMapper;
import com.cartethyia.easyorange.product.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl extends ServiceImpl<ProductImageMapper, ProductImage> implements ProductImageService {

    @Override
    public List<ProductImage> getByProductId(Long productId) {
        return lambdaQuery()
                .eq(ProductImage::getProductId, productId)
                .orderByAsc(ProductImage::getSortOrder)
                .list();
    }

    @Override
    public void saveProductImages(Long productId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        List<ProductImage> images = new ArrayList<>(imageUrls.size());
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(ProductImage.builder()
                    .productId(productId)
                    .imageUrl(imageUrls.get(i))
                    .sortOrder(i)
                    .isMain(i == 0 ? ProductConstants.IMAGE_IS_MAIN : ProductConstants.IMAGE_NOT_MAIN)
                    .build());
        }
        saveBatch(images);
    }

    @Override
    public void deleteByProductId(Long productId) {
        remove(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, productId));
    }

    @Override
    public List<ProductImage> listByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<ProductImage>()
                .in(ProductImage::getProductId, productIds)
                .orderByAsc(ProductImage::getSortOrder));
    }
}
