package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.valueobject.*;

import java.math.BigDecimal;
import java.util.List;

public final class ProductTestFixture {

    private ProductTestFixture() {}

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
            return new ProductCreateSpec(sellerId, categoryId, title, price, null, stock,
                    conditionLevel, location, contactMethod, description, images);
        }
    }

    public static ProductCreateSpec defaultCreateSpec() {
        return aProduct().build();
    }

    public static Product defaultProduct() {
        var t = Product.create(defaultCreateSpec());
        return t.aggregate().assignId("1");
    }

    public static Product onlineProduct() {
        var t = Product.create(defaultCreateSpec());
        var p = t.aggregate().assignId("1");
        return p.putOnline().aggregate();
    }
}
