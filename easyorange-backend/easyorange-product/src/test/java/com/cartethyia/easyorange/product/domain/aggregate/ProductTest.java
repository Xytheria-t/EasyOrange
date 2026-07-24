package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductMarkedSoldEvent;
import com.cartethyia.easyorange.product.domain.event.StockDecreasedEvent;
import com.cartethyia.easyorange.product.domain.event.StockRestoredEvent;
import com.cartethyia.easyorange.product.domain.exception.InsufficientStockException;
import com.cartethyia.easyorange.product.domain.exception.InvalidProductStatusException;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.valueobject.*;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

    @Test
    @DisplayName("创建商品时应生成 ProductCreatedEvent")
    void create_shouldEmitProductCreatedEvent() {
        var result = Product.create(ProductTestFixture.defaultCreateSpec());

        assertThat(result.event()).isInstanceOf(ProductCreatedEvent.class);
    }

    @Test
    @DisplayName("创建商品时名称不能为空")
    void create_withNullTitle_shouldThrow() {
        assertThatThrownBy(() -> Product.create(
                ProductTestFixture.aProduct().withNoTitle().build()
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("创建商品时价格必须大于0")
    void create_withZeroPrice_shouldThrow() {
        assertThatThrownBy(() -> Product.create(
                ProductTestFixture.aProduct().price(BigDecimal.ZERO).build()
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("创建商品时图片不能为空")
    void create_withEmptyImages_shouldThrow() {
        assertThatThrownBy(() -> Product.create(
                ProductTestFixture.aProduct().emptyImages().build()
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("库存不足时应抛出 InsufficientStockException")
    void decrementStock_whenNoStock_shouldThrow() {
        var result = Product.create(ProductTestFixture.aProduct().stock(0).build());

        assertThatThrownBy(() -> result.product().decrementStock())
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("扣减库存成功时应减少库存并发布事件")
    void decrementStock_shouldDecreaseAndEmitEvent() {
        var product = ProductTestFixture.defaultProduct();

        var result = product.decrementStock();

        assertThat(result.product().getStock().value()).isEqualTo(9);
        assertThat(result.event()).isInstanceOf(StockDecreasedEvent.class);
    }

    @Test
    @DisplayName("恢复库存成功时应增加库存并发布事件")
    void restoreStock_shouldIncreaseAndEmitEvent() {
        var product = ProductTestFixture.defaultProduct();

        var result = product.restoreStock();

        assertThat(result.product().getStock().value()).isEqualTo(11);
        assertThat(result.event()).isInstanceOf(StockRestoredEvent.class);
    }

    @Test
    @DisplayName("标记售出成功时应更改状态并发布事件")
    void markAsSold_shouldChangeStatusAndEmitEvent() {
        var product = ProductTestFixture.defaultProduct();
        product = product.putOnline().product();

        var result = product.markAsSold();

        assertThat(result.product().getStatus()).isEqualTo(ProductStatus.SOLD);
        assertThat(result.event()).isInstanceOf(ProductMarkedSoldEvent.class);
    }

    @Test
    @DisplayName("非上架商品不能标记为已售")
    void markAsSold_whenNotOnline_shouldThrow() {
        var product = ProductTestFixture.defaultProduct();

        assertThatThrownBy(product::markAsSold)
                .isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("库存为0时不能上架")
    void putOnline_whenNoStock_shouldThrow() {
        var result = Product.create(ProductTestFixture.aProduct().stock(0).build());

        assertThatThrownBy(() -> result.product().putOnline())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");
    }

    @Test
    @DisplayName("已售商品不能再次标记为已售")
    void markAsSold_whenAlreadySold_shouldThrow() {
        var product = ProductTestFixture.defaultProduct();
        product = product.putOnline().product();
        var soldProduct = product.markAsSold().product();

        assertThatThrownBy(soldProduct::markAsSold)
                .isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("更新商品信息应修改对应字段")
    void update_shouldModifyFields() {
        var product = ProductTestFixture.defaultProduct();

        var result = product.update(
                updateWith(CategoryId.of("99"), ProductTitle.of("新名称"), Money.of(new BigDecimal("200")))
        );

        assertThat(result.product().getCategoryId().value()).isEqualTo("99");
        assertThat(result.product().getTitle().value()).isEqualTo("新名称");
        assertThat(result.product().getPrice().value()).isEqualByComparingTo(new BigDecimal("200"));
    }

    private static ProductUpdateSpec updateWith(CategoryId categoryId, ProductTitle title, Money price) {
        return new ProductUpdateSpec(categoryId, title, price, null, null, null, null, null, null, null);
    }
}
