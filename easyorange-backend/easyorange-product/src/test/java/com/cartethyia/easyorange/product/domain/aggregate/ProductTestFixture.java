package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product 测试夹具 — 提供默认创建参数与典型聚合根状态，减少跨测试类重复构造。
 */
public final class ProductTestFixture {

    private ProductTestFixture() {
    }

    public static ProductCreateSpec defaultCreateSpec() {
        return new ProductCreateSpec(
                SellerId.of("1"),
                CategoryId.of("2"),
                ProductTitle.of("测试商品"),
                Money.of(new BigDecimal("100")),
                null,
                StockQuantity.of(10),
                ConditionLevel.NEW,
                TradeLocation.of("北京"),
                ContactMethod.of("微信"),
                ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg"))
        );
    }

    public static Product defaultProduct() {
        return Product.create(defaultCreateSpec()).product().assignId("1");
    }

    public static Product onlineProduct() {
        return defaultProduct().putOnline().product();
    }
}
