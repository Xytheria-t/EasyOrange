package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record ImageSet(List<ProductImage> images) {

    public static final ImageSet EMPTY = new ImageSet(Collections.emptyList());

    public ImageSet {
        if (images == null || images.isEmpty()) {
            images = Collections.emptyList();
        } else {
            long mainCount = images.stream().filter(ProductImage::isMain).count();
            BizRequire.requireTrue(mainCount <= 1, "主图只能有一个");
            images = List.copyOf(images);
        }
    }

    public static ImageSet empty() {
        return EMPTY;
    }

    public static ImageSet of(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return EMPTY;
        }
        List<ProductImage> imageList = new ArrayList<>(urls.size());
        for (int i = 0; i < urls.size(); i++) {
            imageList.add(new ProductImage(new ImageUrl(urls.get(i)), i, i == 0));
        }
        return new ImageSet(imageList);
    }

    public static ImageSet ofImages(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return EMPTY;
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
                .collect(Collectors.toList());
    }

    public int size() {
        return images.size();
    }

    public boolean isEmpty() {
        return images.isEmpty();
    }

    public record ProductImage(ImageUrl url, Integer sortOrder, boolean isMain) { }
}
