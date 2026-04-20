package com.cartethyia.easyorange.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cartethyia.easyorange.product.entity.ProductImage;

import java.util.List;

public interface ProductImageService extends IService<ProductImage> {

    List<ProductImage> getByProductId(Long productId);

    void saveProductImages(Long productId, List<String> imageUrls);

    void deleteByProductId(Long productId);

    List<ProductImage> listByProductIds(List<Long> productIds);
}
