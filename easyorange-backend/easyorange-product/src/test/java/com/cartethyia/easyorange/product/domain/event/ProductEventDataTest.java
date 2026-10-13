package com.cartethyia.easyorange.product.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.ProductCreateSpec;
import com.cartethyia.easyorange.product.domain.aggregate.ProductUpdateSpec;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ProductEvent.Data 的字段提取契约 —— 创建与更新事件共用同一载体，调用方不判 null，
 * 提取错（把 null 写成空串、或反过来）只会在消费端静默丢字段，故逐分支钉住。
 */
class ProductEventDataTest {

    /** 除必填（title / price / images）外选填全 null —— 覆盖 Data.from 的 null 分支。 */
    private static Product minimalProduct() {
        ProductCreateSpec spec = new ProductCreateSpec(
                SellerId.of("1"),
                null,
                ProductTitle.of("测试商品"),
                Money.of(new BigDecimal("100")),
                null,
                StockQuantity.of(1),
                null,
                null,
                null,
                null,
                ImageSet.of(List.of("http://img/1.jpg")),
                null);
        return Product.create(spec).aggregate().assignId("p-1");
    }

    @Test
    @DisplayName("选填字段为 null 时 Data 对应字段为 null（不是空串 / 0 占位）")
    void from_withNullOptionals_shouldExtractNulls() {
        ProductEvent.Data data = ProductEvent.Data.from(minimalProduct());

        assertThat(data.productId()).isEqualTo("p-1");
        assertThat(data.userId()).isEqualTo("1");
        assertThat(data.categoryId()).isNull();
        assertThat(data.originalPrice()).isNull();
        assertThat(data.conditionLevel()).isNull();
        assertThat(data.location()).isNull();
        assertThat(data.description()).isNull();
        assertThat(data.imageUrls()).containsExactly("http://img/1.jpg");
    }

    @Test
    @DisplayName("联系方式 trim 后为空白时提取为 null —— 空白与「未填」在事件里必须同形")
    void from_withBlankContactMethod_shouldExtractNull() {
        var updated = minimalProduct()
                .update(
                        "1",
                        new ProductUpdateSpec(
                                null, null, null, null, null, null, null, new ContactMethod("   "), null, null))
                .aggregate();

        assertThat(ProductEvent.Data.from(updated).contactMethod()).isNull();
    }

    @Test
    @DisplayName("联系方式 / 交易地点有值时提取 trim 后的原文")
    void from_withValues_shouldExtractTrimmedValues() {
        var updated = minimalProduct()
                .update(
                        "1",
                        new ProductUpdateSpec(
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                new TradeLocation("北京"),
                                new ContactMethod("  微信  "),
                                null,
                                null))
                .aggregate();

        ProductEvent.Data data = ProductEvent.Data.from(updated);

        assertThat(data.location()).isEqualTo("北京");
        assertThat(data.contactMethod()).isEqualTo("微信");
    }

    @Test
    @DisplayName("imageUrls 为 null 时落空列表（消费端 foreach 不判 null）")
    void compactConstructor_withNullImages_shouldFallbackToEmpty() {
        var data = new ProductEvent.Data("p", "u", "c", "n", null, null, 1, null, null, null, null, null);

        assertThat(data.imageUrls()).isEmpty();
    }

    @Test
    @DisplayName("update 传全 null spec 时原值不动（null = 不改，不是「清空」）")
    void update_withAllNullSpec_shouldKeepOriginalValues() {
        var updated = minimalProduct()
                .update("1", new ProductUpdateSpec(null, null, null, null, null, null, null, null, null, null))
                .aggregate();

        assertThat(updated.getTitle().value()).isEqualTo("测试商品");
        assertThat(updated.getPrice().value()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(updated.getImages().imageUrls()).containsExactly("http://img/1.jpg");
    }

    @Test
    @DisplayName("update 的事件携带新值（创建与更新共用载体不漂移）")
    void update_shouldCarryNewValuesInEventData() {
        var transition = minimalProduct()
                .update(
                        "1",
                        new ProductUpdateSpec(
                                CategoryId.of("9"),
                                ProductTitle.of("新标题"),
                                Money.of(new BigDecimal("200")),
                                null,
                                StockQuantity.of(5),
                                null,
                                null,
                                null,
                                null,
                                null));

        assertThat(transition.event()).isInstanceOf(ProductUpdatedEvent.class);
        assertThat(transition.event().data().name()).isEqualTo("新标题");
        assertThat(transition.event().data().price()).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(transition.event().data().stock()).isEqualTo(5);
    }

    @Test
    @DisplayName("创建事件的 data 与 Data.from(aggregate) 同源")
    void createdEvent_dataShouldMatchFromAggregate() {
        var transition = Product.create(new ProductCreateSpec(
                SellerId.of("1"),
                CategoryId.of("2"),
                ProductTitle.of("测试商品"),
                Money.of(new BigDecimal("100")),
                null,
                StockQuantity.of(1),
                null,
                null,
                null,
                null,
                ImageSet.of(List.of("http://img/1.jpg")),
                null));

        assertThat(transition.event().data()).isEqualTo(ProductEvent.Data.from(transition.aggregate()));
    }
}
