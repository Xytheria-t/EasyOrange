package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductTagger {

    private final CreditScoreFetcher creditScoreFetcher;

    private static final int CREDIT_THRESHOLD = 120;
    private static final int DISCOUNT_THRESHOLD = 10;
    private static final int IMAGE_THRESHOLD = 3;

    private static final String TAG_DISCOUNT = "💰超值";
    private static final String TAG_IMAGE = "📸实拍";
    private static final String TAG_CREDIT = "⭐信用优";

    public Map<String, List<String>> tagProducts(List<ProductReadModel> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }

        Set<String> sellerIds =
                products.stream().map(ProductReadModel::sellerId).collect(Collectors.toSet());

        Map<String, Integer> creditScores = creditScoreFetcher.fetchCreditScores(sellerIds);

        Map<String, List<String>> tags = new HashMap<>(products.size());
        for (var product : products) {
            List<String> productTags = new ArrayList<>(4);
            tagDiscount(product, productTags);
            tagImages(product, productTags);
            tagCredit(product, creditScores, productTags);
            tags.put(product.id(), List.copyOf(productTags));
        }
        return Collections.unmodifiableMap(tags);
    }

    private void tagDiscount(ProductReadModel product, List<String> productTags) {
        BigDecimal originalPrice = product.originalPrice();
        BigDecimal price = product.price();
        if (originalPrice != null
                && originalPrice.compareTo(BigDecimal.ZERO) > 0
                && price != null
                && originalPrice.compareTo(price) > 0) {
            int discountPercent = price.multiply(BigDecimal.valueOf(100))
                    .divide(originalPrice, 0, java.math.RoundingMode.HALF_UP)
                    .intValue();
            if (discountPercent <= (100 - DISCOUNT_THRESHOLD)) {
                productTags.add(TAG_DISCOUNT);
            }
        }
    }

    private void tagImages(ProductReadModel product, List<String> productTags) {
        List<String> images = product.images();
        if (images != null && images.size() >= IMAGE_THRESHOLD) {
            productTags.add(TAG_IMAGE);
        }
    }

    private void tagCredit(ProductReadModel product, Map<String, Integer> creditScores, List<String> productTags) {
        try {
            Integer score = creditScores.get(product.sellerId());
            if (score != null && score >= CREDIT_THRESHOLD) {
                productTags.add(TAG_CREDIT);
            }
        } catch (Exception e) {
            log.debug(
                    "Credit tag skipped for productId={}, sellerId={}: {}",
                    product.id(),
                    product.sellerId(),
                    e.getMessage());
        }
    }
}
