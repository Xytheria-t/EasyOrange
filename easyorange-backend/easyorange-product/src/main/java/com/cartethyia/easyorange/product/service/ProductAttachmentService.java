package com.cartethyia.easyorange.product.service;

import com.cartethyia.easyorange.product.entity.ProductDetail;
import com.cartethyia.easyorange.product.entity.ProductImage;

import java.util.List;

public interface ProductAttachmentService {

    ProductDetail getDetailByProductId(Long productId);

    void saveDetail(ProductDetail detail);

    void updateDetail(ProductDetail detail);

    void deleteDetailByProductId(Long productId);

    List<ProductImage> listImagesByProductIds(List<Long> productIds);

    void saveImages(Long productId, List<ProductImage> images);

    void deleteImagesByProductId(Long productId);

    record AggregateData(ProductDetail detail, List<ProductImage> images) {}

    AggregateData loadAggregateData(Long productId);
}