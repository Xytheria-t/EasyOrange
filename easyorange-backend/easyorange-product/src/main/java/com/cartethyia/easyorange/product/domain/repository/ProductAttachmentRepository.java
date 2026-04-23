package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.entity.ProductDetail;
import com.cartethyia.easyorange.product.entity.ProductImage;

import java.util.List;

public interface ProductAttachmentRepository {

    ProductDetail findDetailByProductId(Long productId);

    void saveDetail(ProductDetail detail);

    void updateDetail(ProductDetail detail);

    void deleteDetailByProductId(Long productId);

    List<ProductImage> findImagesByProductIds(List<Long> productIds);

    void saveImages(Long productId, List<ProductImage> images);

    void deleteImagesByProductId(Long productId);
}
