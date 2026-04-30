package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductMarkedSoldEvent;
import com.cartethyia.easyorange.product.domain.event.StockDecreasedEvent;
import com.cartethyia.easyorange.product.domain.event.StockRestoredEvent;
import com.cartethyia.easyorange.product.domain.exception.InsufficientStockException;
import com.cartethyia.easyorange.product.domain.exception.InvalidProductStatusException;
import com.cartethyia.easyorange.product.domain.valueobject.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

    private Product createDefaultProduct() {
        return Product.create(
                SellerId.of(1L),
                CategoryId.of(2L),
                ProductTitle.of("测试商品"),
                Money.of(new BigDecimal("100")),
                null,
                StockQuantity.of(10),
                ConditionLevelVO.of(1),
                TradeLocation.of("北京"),
                ContactMethod.of("微信"),
                ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg"))
        );
    }

    @Test
    @DisplayName("创建商品时应生成 ProductCreatedEvent")
    void create_shouldEmitProductCreatedEvent() {
        Product product = createDefaultProduct();

        assertThat(product.getId()).isNotNull();
        assertThat(product.releaseEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(ProductCreatedEvent.class);
    }

    @Test
    @DisplayName("创建商品时名称不能为空")
    void create_withNullTitle_shouldThrow() {
        assertThatThrownBy(() -> Product.create(
                SellerId.of(1L), CategoryId.of(2L), null,
                Money.of(new BigDecimal("100")), null, StockQuantity.of(10),
                ConditionLevelVO.of(1), TradeLocation.of("北京"),
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
                ConditionLevelVO.of(1), TradeLocation.of("北京"),
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
                ConditionLevelVO.of(1), TradeLocation.of("北京"),
                ContactMethod.of("微信"), ProductDescription.of("描述"),
                ImageSet.empty()
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("库存不足时应抛出 InsufficientStockException")
    void decrementStock_whenNoStock_shouldThrow() {
        Product product = Product.create(
                SellerId.of(1L), CategoryId.of(2L), ProductTitle.of("商品"),
                Money.of(new BigDecimal("100")), null, StockQuantity.of(0),
                ConditionLevelVO.of(1), TradeLocation.of("北京"),
                ContactMethod.of("微信"), ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg"))
        );
        product.releaseEvents();

        assertThatThrownBy(product::decrementStock)
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("扣减库存成功时应减少库存并发布事件")
    void decrementStock_shouldDecreaseAndEmitEvent() {
        Product product = createDefaultProduct();
        product.releaseEvents();

        product.decrementStock();

        assertThat(product.getStock().value()).isEqualTo(9);
        assertThat(product.releaseEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(StockDecreasedEvent.class);
    }

    @Test
    @DisplayName("恢复库存成功时应增加库存并发布事件")
    void restoreStock_shouldIncreaseAndEmitEvent() {
        Product product = createDefaultProduct();
        product.releaseEvents();

        product.restoreStock();

        assertThat(product.getStock().value()).isEqualTo(11);
        assertThat(product.releaseEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(StockRestoredEvent.class);
    }

    @Test
    @DisplayName("标记售出成功时应更改状态并发布事件")
    void markAsSold_shouldChangeStatusAndEmitEvent() {
        Product product = createDefaultProduct();
        product.releaseEvents();

        product.markAsSold();

        assertThat(product.getStatus().code()).isEqualTo(2);
        assertThat(product.releaseEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(ProductMarkedSoldEvent.class);
    }

    @Test
    @DisplayName("非上架商品不能标记为已售")
    void markAsSold_whenNotOnline_shouldThrow() {
        Product product = createDefaultProduct();
        product.markAsSold();
        product.releaseEvents();

        assertThatThrownBy(product::markAsSold)
                .isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("更新商品信息应修改对应字段")
    void update_shouldModifyFields() {
        Product product = createDefaultProduct();
        product.releaseEvents();

        product.update(
                CategoryId.of(99L),
                ProductTitle.of("新名称"),
                Money.of(new BigDecimal("200")),
                null, null, null, null, null, null, null
        );

        assertThat(product.getCategoryId().value()).isEqualTo(99L);
        assertThat(product.getTitle().value()).isEqualTo("新名称");
        assertThat(product.getPrice().value()).isEqualByComparingTo(new BigDecimal("200"));
    }

    @Test
    @DisplayName("releaseEvents 后事件列表应清空")
    void releaseEvents_shouldClearEventList() {
        Product product = createDefaultProduct();

        var events = product.releaseEvents();
        assertThat(events).hasSize(1);

        var secondRelease = product.releaseEvents();
        assertThat(secondRelease).isEmpty();
    }
}
