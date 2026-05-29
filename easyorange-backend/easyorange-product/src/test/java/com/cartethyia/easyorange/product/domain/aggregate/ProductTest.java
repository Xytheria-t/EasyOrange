package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductCreatedResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductMarkedSoldResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.StockDecreasedResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.StockRestoredResult;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductMarkedSoldEvent;
import com.cartethyia.easyorange.product.domain.event.StockDecreasedEvent;
import com.cartethyia.easyorange.product.domain.event.StockRestoredEvent;
import com.cartethyia.easyorange.product.domain.exception.InsufficientStockException;
import com.cartethyia.easyorange.product.domain.exception.InvalidProductStatusException;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.valueobject.*;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

    private Product createDefaultProduct() {
        ProductCreatedResult result = Product.create(
                SellerId.of(1L),
                CategoryId.of(2L),
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
        return result.product().assignId(1L);
    }

    @Test
    @DisplayName("创建商品时应生成 ProductCreatedEvent")
    void create_shouldEmitProductCreatedEvent() {
        ProductCreatedResult result = Product.create(
                SellerId.of(1L), CategoryId.of(2L), ProductTitle.of("测试商品"),
                Money.of(new BigDecimal("100")), null, StockQuantity.of(10),
                ConditionLevel.NEW, TradeLocation.of("北京"),
                ContactMethod.of("微信"), ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg"))
        );

        assertThat(result.event()).isInstanceOf(ProductCreatedEvent.class);
    }

    @Test
    @DisplayName("创建商品时名称不能为空")
    void create_withNullTitle_shouldThrow() {
        assertThatThrownBy(() -> Product.create(
                SellerId.of(1L), CategoryId.of(2L), null,
                Money.of(new BigDecimal("100")), null, StockQuantity.of(10),
                ConditionLevel.NEW, TradeLocation.of("北京"),
                ContactMethod.of("微信"), ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg"))
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("创建商品时价格必须大于0")
    void create_withZeroPrice_shouldThrow() {
        assertThatThrownBy(() -> Product.create(
                SellerId.of(1L), CategoryId.of(2L), ProductTitle.of("商品"),
                Money.of(BigDecimal.ZERO), null, StockQuantity.of(10),
                ConditionLevel.NEW, TradeLocation.of("北京"),
                ContactMethod.of("微信"), ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg"))
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("创建商品时图片不能为空")
    void create_withEmptyImages_shouldThrow() {
        assertThatThrownBy(() -> Product.create(
                SellerId.of(1L), CategoryId.of(2L), ProductTitle.of("商品"),
                Money.of(new BigDecimal("100")), null, StockQuantity.of(10),
                ConditionLevel.NEW, TradeLocation.of("北京"),
                ContactMethod.of("微信"), ProductDescription.of("描述"),
                ImageSet.empty()
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("库存不足时应抛出 InsufficientStockException")
    void decrementStock_whenNoStock_shouldThrow() {
        ProductCreatedResult result = Product.create(
                SellerId.of(1L), CategoryId.of(2L), ProductTitle.of("商品"),
                Money.of(new BigDecimal("100")), null, StockQuantity.of(0),
                ConditionLevel.NEW, TradeLocation.of("北京"),
                ContactMethod.of("微信"), ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg"))
        );

        assertThatThrownBy(() -> result.product().decrementStock())
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("扣减库存成功时应减少库存并发布事件")
    void decrementStock_shouldDecreaseAndEmitEvent() {
        Product product = createDefaultProduct();

        StockDecreasedResult result = product.decrementStock();

        assertThat(result.product().getStock().value()).isEqualTo(9);
        assertThat(result.event()).isInstanceOf(StockDecreasedEvent.class);
    }

    @Test
    @DisplayName("恢复库存成功时应增加库存并发布事件")
    void restoreStock_shouldIncreaseAndEmitEvent() {
        Product product = createDefaultProduct();

        StockRestoredResult result = product.restoreStock();

        assertThat(result.product().getStock().value()).isEqualTo(11);
        assertThat(result.event()).isInstanceOf(StockRestoredEvent.class);
    }

    @Test
    @DisplayName("标记售出成功时应更改状态并发布事件")
    void markAsSold_shouldChangeStatusAndEmitEvent() {
        Product product = createDefaultProduct();
        product = product.putOnline();

        ProductMarkedSoldResult result = product.markAsSold();

        assertThat(result.product().getStatus().getCode()).isEqualTo(2);
        assertThat(result.event()).isInstanceOf(ProductMarkedSoldEvent.class);
    }

    @Test
    @DisplayName("非上架商品不能标记为已售")
    void markAsSold_whenNotOnline_shouldThrow() {
        Product product = createDefaultProduct();

        assertThatThrownBy(() -> product.markAsSold())
                .isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("库存为0时不能上架")
    void putOnline_whenNoStock_shouldThrow() {
        ProductCreatedResult result = Product.create(
                SellerId.of(1L), CategoryId.of(2L), ProductTitle.of("商品"),
                Money.of(new BigDecimal("100")), null, StockQuantity.of(0),
                ConditionLevel.NEW, TradeLocation.of("北京"),
                ContactMethod.of("微信"), ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg"))
        );

        assertThatThrownBy(() -> result.product().putOnline())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");
    }

    @Test
    @DisplayName("已售商品不能再次标记为已售")
    void markAsSold_whenAlreadySold_shouldThrow() {
        Product product = createDefaultProduct();
        product = product.putOnline();
        Product soldProduct = product.markAsSold().product();

        assertThatThrownBy(() -> soldProduct.markAsSold())
                .isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("更新商品信息应修改对应字段")
    void update_shouldModifyFields() {
        Product product = createDefaultProduct();

        Product.ProductUpdatedResult result = product.update(
                CategoryId.of(99L),
                ProductTitle.of("新名称"),
                Money.of(new BigDecimal("200")),
                null, null, null, null, null, null, null
        );

        assertThat(result.product().getCategoryId().value()).isEqualTo(99L);
        assertThat(result.product().getTitle().value()).isEqualTo("新名称");
        assertThat(result.product().getPrice().value()).isEqualByComparingTo(new BigDecimal("200"));
    }
}