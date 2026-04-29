package com.cartethyia.easyorange.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.FileUtils;
import com.cartethyia.easyorange.product.constant.ProductConstant;
import com.cartethyia.easyorange.product.entity.ProductImage;
import com.cartethyia.easyorange.product.mapper.ProductImageMapper;
import com.cartethyia.easyorange.product.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl extends ServiceImpl<ProductImageMapper, ProductImage> implements ProductImageService {

    @Value("${product.image.path:./upload/product}")
    private String imageUploadPath;

    @Override
    public List<ProductImage> getByProductId(Long productId) {
        BizRequire.notNull(productId, "商品 ID 不能为空");
        BizRequire.positive(productId, "商品 ID 必须为正数");
        return lambdaQuery()
                .eq(ProductImage::getProductId, productId)
                .orderByAsc(ProductImage::getSortOrder)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProductImages(Long productId, List<String> imageUrls) {
        BizRequire.notNull(productId, "商品 ID 不能为空");
        BizRequire.positive(productId, "商品 ID 必须为正数");
        BizRequire.notEmpty(imageUrls, "图片 URL 列表不能为空");
        BizRequire.noNullElements(imageUrls, "图片 URL 列表不能包含空元素");
        
        List<ProductImage> images = new ArrayList<>(imageUrls.size());
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(ProductImage.builder()
                    .productId(productId)
                    .imageUrl(imageUrls.get(i))
                    .sortOrder(i)
                    .isMain(i == 0 ? ProductConstant.IMAGE_IS_MAIN : ProductConstant.IMAGE_NOT_MAIN)
                    .build());
        }
        saveBatch(images);
    }

    @Override
    public List<ProductImage> listByProductIds(List<Long> productIds) {
        BizRequire.notEmpty(productIds, "商品 ID 列表不能为空");
        BizRequire.noNullElements(productIds, "商品 ID 列表不能包含空元素");
        return list(new LambdaQueryWrapper<ProductImage>()
                .in(ProductImage::getProductId, productIds)
                .orderByAsc(ProductImage::getSortOrder));
    }

    @Override
    public String uploadProductImage(Long productId, MultipartFile image) {
        BizRequire.notNull(productId, "商品 ID 不能为空");
        BizRequire.positive(productId, "商品 ID 必须为正数");
        BizRequire.notNull(image, "上传的图片不能为空");
        BizRequire.requireTrue(!image.isEmpty(), "上传的图片不能为空");
        
        try {
            String[] allowedExtensions = new String[]{"jpg", "jpeg", "png", "webp"};
            String imagePath = FileUtils.upload(imageUploadPath, image, allowedExtensions);
            log.info("action=upload_product_image, productId={}, imagePath={}", productId, imagePath);
            return imagePath;
        } catch (IOException e) {
            log.error("上传商品图片失败：productId={}, error={}", productId, e.getMessage());
            throw new RuntimeException("上传商品图片失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void deleteProductImageFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        String fullPath = imageUploadPath + File.separator + imageUrl;
        if (FileUtils.exists(fullPath)) {
            boolean deleted = FileUtils.deleteFile(fullPath);
            if (deleted) {
                log.info("action=delete_product_image, imageUrl={}", imageUrl);
            } else {
                log.warn("action=delete_product_image_failed, imageUrl={}", imageUrl);
            }
        } else {
            log.warn("action=check_product_image_not_exists, fullPath={}", fullPath);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByProductId(Long productId) {
        BizRequire.notNull(productId, "商品 ID 不能为空");
        BizRequire.positive(productId, "商品 ID 必须为正数");
        
        List<ProductImage> images = getByProductId(productId);

        for (ProductImage image : images) {
            deleteProductImageFile(image.getImageUrl());
        }

        remove(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, productId));

        log.info("action=delete_product_images, productId={}, count={}", productId, images.size());
    }
}
