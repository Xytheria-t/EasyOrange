package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record ImageSet(List<ProductImageVO> images) {
    public ImageSet {
        if (images == null || images.isEmpty()) {
            images = Collections.emptyList();
        } else {
            long mainCount = images.stream()
                    .filter(ProductImageVO::isMain)
                    .count();
            BizRequire.requireTrue(mainCount <= 1, "主图只能有一个");
            List<ProductImageVO> validated = new ArrayList<>();
            for (ProductImageVO img : images) {
                validated.add(new ProductImageVO(
                        img.url(),
                        img.sortOrder(),
                        img.isMain()
                ));
            }
            images = Collections.unmodifiableList(validated);
        }
    }

    public static ImageSet empty() {
        return new ImageSet(Collections.emptyList());
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

    public record ProductImageVO(ImageUrl url, Integer sortOrder, boolean isMain) {
        public boolean isMain() {
            return isMain;
        }
    }
}