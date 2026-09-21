package com.cartethyia.easyorange.ai.application.enhancement;

import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 商品标签规则引擎 — 折扣（{@code 💰超值}）与实拍（{@code 📸实拍}）两个标签的本地计算，
 * 零 LLM 调用、亚毫秒返回，产物是 {@code productId → 标签列表} 供搜索增强的 product_tagging 工具消费。
 * <p>
 * 这两个判据本来就是确定性计算（现价与原价的关系、图片张数），交给模型只会多付一次调用、
 * 换来一个不确定的答案；标签因此走规则，AI 侧只负责把它和其余 3 路增强一起拼进响应。
 * <p>
 * 判据：折扣按现价占原价的百分比四舍五入后 {@code <= 90}（即降幅 ≥ {@code DISCOUNT_THRESHOLD}%），
 * 原价缺失 / 非正 / 现价缺失都不出标签；图片数 {@code >= IMAGE_THRESHOLD} 出实拍标签。
 */
@Component
public class ProductTagger {

    private static final int DISCOUNT_THRESHOLD = 10;
    private static final int IMAGE_THRESHOLD = 3;

    private static final String TAG_DISCOUNT = "💰超值";
    private static final String TAG_IMAGE = "📸实拍";

    public Map<String, List<String>> tagProducts(List<ProductReadModel> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> tags = new HashMap<>(products.size());
        for (var product : products) {
            List<String> productTags = new ArrayList<>(4);
            tagDiscount(product, productTags);
            tagImages(product, productTags);
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
                    .divide(originalPrice, 0, RoundingMode.HALF_UP)
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
}
