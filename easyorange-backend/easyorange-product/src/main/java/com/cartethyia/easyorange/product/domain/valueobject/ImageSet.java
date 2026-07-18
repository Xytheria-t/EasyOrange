package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.ArrayList;
import java.util.List;

public record ImageSet(List<ProductImage> images) {

    public ImageSet {
        if (images == null || images.isEmpty()) {
            images = List.of();
        } else {
            long mainCount = images.stream().filter(ProductImage::isMain).count();
            BizRequire.requireTrue(mainCount <= 1, "主图只能有一个");
            images = List.copyOf(images);
        }
    }

    public static ImageSet empty() {
        return new ImageSet(List.of());
    }

    public static ImageSet of(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return empty();
        }
        List<ProductImage> imageList = new ArrayList<>(urls.size());
        for (int i = 0; i < urls.size(); i++) {
            imageList.add(new ProductImage(new ImageUrl(urls.get(i)), i, i == 0));
        }
        return new ImageSet(imageList);
    }

    public static ImageSet ofImages(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return empty();
        }
        return new ImageSet(images);
    }

    public ImageUrl mainImage() {
        return images.stream()
                .filter(ProductImage::isMain)
                .map(ProductImage::url)
                .findFirst()
                .orElse(null);
    }

    public List<String> imageUrls() {
        return images.stream()
                .map(img -> img.url() != null ? img.url().value() : null)
                .toList();
    }

    public int size() {
        return images.size();
    }

    public boolean isEmpty() {
        return images.isEmpty();
    }

    public record ProductImage(ImageUrl url, Integer sortOrder, boolean isMain) { }
}
