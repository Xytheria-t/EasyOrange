package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ImageSet implements ValueObject {

    private final List<ProductImageVO> images;

    public ImageSet(List<ProductImageVO> images) {
        if (images == null || images.isEmpty()) {
            this.images = Collections.emptyList();
            return;
        }
        List<ProductImageVO> validated = new ArrayList<>();
        long mainCount = images.stream()
                .filter(ProductImageVO::isMain)
                .count();
        BizRequire.isTrue(mainCount <= 1, "主图只能有一个");
        for (ProductImageVO img : images) {
            validated.add(new ProductImageVO(
                    img.url(),
                    img.sortOrder(),
                    img.isMain()
            ));
        }
        this.images = Collections.unmodifiableList(validated);
    }

    public static ImageSet empty() {
        return new ImageSet(Collections.emptyList());
    }

    public List<ProductImageVO> images() {
        return images;
    }

    public ImageUrl mainImage() {
        return images.stream()
                .filter(ProductImageVO::isMain)
                .map(ProductImageVO::url)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageSet imageSet = (ImageSet) o;
        return Objects.equals(images, imageSet.images);
    }

    @Override
    public int hashCode() {
        return Objects.hash(images);
    }

    public record ProductImageVO(ImageUrl url, Integer sortOrder, boolean isMain) {
        public boolean isMain() {
            return isMain;
        }
    }
}
