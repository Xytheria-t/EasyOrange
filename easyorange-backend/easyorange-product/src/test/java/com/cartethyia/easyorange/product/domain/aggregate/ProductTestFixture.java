package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.valueobject.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product 测试夹具 — Test Data Builder 模式 + 快捷工厂方法。
 * <p>
 * 推荐用法：
 * <pre>{@code
 * Product.create(aProduct().build());                     // 默认参数
 * Product.create(aProduct().stock(0).build());            // 零库存
 * Product.create(aProduct().withNoTitle().build());       // 无标题（校验测试）
 * Product.create(aProduct().price(BigDecimal.ZERO).build());     // 零价格
 * Product.create(aProduct().emptyImages().build());       // 无图片
 * }</pre>
 */
public final class ProductTestFixture {

    private ProductTestFixture() {}

    // ==================== Test Data Builder ====================

    public static ProductCreateSpecBuilder aProduct() {
        return new ProductCreateSpecBuilder();
    }

    public static class ProductCreateSpecBuilder {
        private final SellerId sellerId = SellerId.of("1");
        private final CategoryId categoryId = CategoryId.of("2");
        private ProductTitle title = ProductTitle.of("测试商品");
        private Money price = Money.of(new BigDecimal("100"));
        private StockQuantity stock = StockQuantity.of(10);
        private final ConditionLevel conditionLevel = ConditionLevel.NEW;
        private final TradeLocation location = TradeLocation.of("北京");
        private final ContactMethod contactMethod = ContactMethod.of("微信");
        private final ProductDescription description = ProductDescription.of("描述");
        private ImageSet images = ImageSet.of(List.of("http://img/1.jpg"));

        private ProductCreateSpecBuilder() {}

        public ProductCreateSpecBuilder stock(int value) { this.stock = StockQuantity.of(value); return this; }
        public ProductCreateSpecBuilder withNoTitle() { this.title = null; return this; }
        public ProductCreateSpecBuilder price(BigDecimal value) { this.price = Money.of(value); return this; }
        public ProductCreateSpecBuilder emptyImages() { this.images = ImageSet.empty(); return this; }

        public ProductCreateSpec build() {
            return new ProductCreateSpec(sellerId, categoryId, title, price, /* originalPrice */ null, stock,
                    conditionLevel, location, contactMethod, description, images);
        }
    }

    // ==================== Convenience ====================

    public static ProductCreateSpec defaultCreateSpec() {
        return aProduct().build();
    }

    public static Product defaultProduct() {
        return Product.create(defaultCreateSpec()).product().assignId("1");
    }

    public static Product onlineProduct() {
        return defaultProduct().putOnline().product();
    }
}
