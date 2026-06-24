package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.aggregate.Product.PriceAdjustedResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductCreatedResult;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ConsignmentMode;
import com.cartethyia.easyorange.product.domain.event.PriceAdjustedEvent;
import com.cartethyia.easyorange.product.domain.exception.InvalidProductStatusException;
import com.cartethyia.easyorange.product.domain.valueobject.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ProductConsignmentTest {

    private Product createAiManagedProduct() {
        ProductCreatedResult result = Product.create(
                SellerId.of(1L),
                CategoryId.of(2L),
                ProductTitle.of("AI托管商品"),
                Money.of(new BigDecimal("100")),
                null,
                Money.of(new BigDecimal("50")),
                ConsignmentMode.AI_MANAGED,
                StockQuantity.of(10),
                ConditionLevel.NEW,
                TradeLocation.of("北京"),
                ContactMethod.of("微信"),
                ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg"))
        );
        return result.product().assignId(1L);
    }

    private Product createManualProduct() {
        ProductCreatedResult result = Product.create(
                SellerId.of(1L),
                CategoryId.of(2L),
                ProductTitle.of("手动商品"),
                Money.of(new BigDecimal("100")),
                null,
                null,
                ConsignmentMode.MANUAL,
                StockQuantity.of(10),
                ConditionLevel.NEW,
                TradeLocation.of("北京"),
                ContactMethod.of("微信"),
                ProductDescription.of("描述"),
                ImageSet.of(List.of("http://img/1.jpg"))
        );
        return result.product().assignId(1L);
    }

    // ==================== create() tests ====================

    @Test
    @DisplayName("创建 AI 托管商品时字段应正确设置")
    void create_aiManaged_shouldSetFieldsCorrectly() {
        Product product = createAiManagedProduct();

        assertThat(product.getConsignmentMode()).isEqualTo(ConsignmentMode.AI_MANAGED);
        assertThat(product.getFloorPrice()).isNotNull();
        assertThat(product.getFloorPrice().value()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(product.getListedAt()).isNull();
        assertThat(product.getCurrentPriceLevel()).isEqualTo(0);
    }

    @Test
    @DisplayName("创建手动商品时 floorPrice 应为 null, consignmentMode 为 MANUAL")
    void create_manual_shouldHaveNullFloorPrice() {
        Product product = createManualProduct();

        assertThat(product.getConsignmentMode()).isEqualTo(ConsignmentMode.MANUAL);
        assertThat(product.getFloorPrice()).isNull();
        assertThat(product.getListedAt()).isNull();
        assertThat(product.getCurrentPriceLevel()).isEqualTo(0);
    }

    // ==================== putOnline() tests ====================

    @Test
    @DisplayName("上架时应设置 listedAt 和 currentPriceLevel=0")
    void putOnline_shouldSetListedAtAndPriceLevel() {
        Product product = createAiManagedProduct();
        assertThat(product.getListedAt()).isNull();

        Product onlineProduct = product.putOnline();

        assertThat(onlineProduct.getListedAt()).isNotNull();
        assertThat(onlineProduct.getCurrentPriceLevel()).isEqualTo(0);
    }

    // ==================== adjustPrice() tests ====================

    @Test
    @DisplayName("AI 托管商品降价5%时应更新价格和阶梯等级并发布事件")
    void adjustPrice_level1_shouldReducePriceAndEmitEvent() {
        Product product = createAiManagedProduct().putOnline();

        PriceAdjustedResult result = product.adjustPrice(1);

        assertThat(result.product().getPrice().value()).isEqualByComparingTo(new BigDecimal("95.00"));
        assertThat(result.product().getCurrentPriceLevel()).isEqualTo(1);
        assertThat(result.event()).isInstanceOf(PriceAdjustedEvent.class);
        assertThat(result.event().newPrice()).isEqualByComparingTo(new BigDecimal("95.00"));
        assertThat(result.event().priceLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("AI 托管商品降价到底价时应返回 floorPrice")
    void adjustPrice_level3_shouldReturnFloorPrice() {
        Product product = createAiManagedProduct().putOnline();

        PriceAdjustedResult result = product.adjustPrice(3);

        assertThat(result.product().getPrice().value()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.product().getCurrentPriceLevel()).isEqualTo(3);
        assertThat(result.event().newPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.event().priceLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("手动模式商品调价应抛异常")
    void adjustPrice_onManualMode_shouldThrow() {
        Product product = createManualProduct().putOnline();

        assertThatThrownBy(() -> product.adjustPrice(1))
                .isInstanceOf(InvalidProductStatusException.class)
                .hasMessageContaining("AI托管");
    }

    @Test
    @DisplayName("未上架商品调价应抛异常")
    void adjustPrice_onOfflineProduct_shouldThrow() {
        Product product = createAiManagedProduct();

        assertThatThrownBy(() -> product.adjustPrice(1))
                .isInstanceOf(InvalidProductStatusException.class)
                .hasMessageContaining("上架");
    }

    @Test
    @DisplayName("降价后价格不应低于 floorPrice 的边界校验")
    void adjustPrice_level3_shouldNotGoBelowFloorPrice() {
        Product product = createAiManagedProduct().putOnline();

        PriceAdjustedResult result = product.adjustPrice(3);

        // floorPrice is 50, level 3 should give 50 exactly
        assertThat(result.product().getPrice().value()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.product().getPrice().value().compareTo(result.product().getFloorPrice().value())).isEqualTo(0);
    }

    @Test
    @DisplayName("adjustPrice(0) 应保持原价")
    void adjustPrice_level0_shouldKeepOriginalPrice() {
        Product product = createAiManagedProduct().putOnline();

        PriceAdjustedResult result = product.adjustPrice(0);

        assertThat(result.product().getPrice().value()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.product().getCurrentPriceLevel()).isEqualTo(0);
    }

    // ==================== calculateExpectedPriceLevel() tests ====================

    @Test
    @DisplayName("未上架商品 calculateExpectedPriceLevel 应返回0")
    void calculateExpectedPriceLevel_whenNotListed_shouldReturn0() {
        Product product = createAiManagedProduct();

        assertThat(product.calculateExpectedPriceLevel()).isEqualTo(0);
    }

    @Test
    @DisplayName("手动模式商品 calculateExpectedPriceLevel 应返回0")
    void calculateExpectedPriceLevel_whenManual_shouldReturn0() {
        Product product = createManualProduct().putOnline();

        assertThat(product.calculateExpectedPriceLevel()).isEqualTo(0);
    }

    @Test
    @DisplayName("上架0天时应返回阶梯0（原价）")
    void calculateExpectedPriceLevel_0days_shouldReturn0() {
        Product product = createAiManagedProduct().putOnline();

        // listedAt is now, so daysOnline = 0
        assertThat(product.calculateExpectedPriceLevel()).isEqualTo(0);
    }

    // ==================== calculatePriceForLevel() tests ====================

    @Test
    @DisplayName("calculatePriceForLevel(0) 应返回原价")
    void calculatePriceForLevel_0_shouldReturnOriginalPrice() {
        Product product = createAiManagedProduct().putOnline();

        // adjustPrice internally calls calculatePriceForLevel
        PriceAdjustedResult result = product.adjustPrice(0);
        assertThat(result.product().getPrice().value()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("calculatePriceForLevel(1) 应返回原价×0.95")
    void calculatePriceForLevel_1_shouldReturn95Percent() {
        Product product = createAiManagedProduct().putOnline();

        PriceAdjustedResult result = product.adjustPrice(1);
        assertThat(result.product().getPrice().value()).isEqualByComparingTo(new BigDecimal("95.00"));
    }

    @Test
    @DisplayName("calculatePriceForLevel(2) 应返回原价×0.90")
    void calculatePriceForLevel_2_shouldReturn90Percent() {
        Product product = createAiManagedProduct().putOnline();

        PriceAdjustedResult result = product.adjustPrice(2);
        assertThat(result.product().getPrice().value()).isEqualByComparingTo(new BigDecimal("90.00"));
    }

    @Test
    @DisplayName("calculatePriceForLevel(3) 应返回 floorPrice")
    void calculatePriceForLevel_3_shouldReturnFloorPrice() {
        Product product = createAiManagedProduct().putOnline();

        PriceAdjustedResult result = product.adjustPrice(3);
        assertThat(result.product().getPrice().value()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    // ==================== event field mapping ====================

    @Test
    @DisplayName("PriceAdjustedEvent 应包含正确的字段映射")
    void priceAdjustedEvent_shouldMapFields() {
        Product product = createAiManagedProduct().putOnline();
        PriceAdjustedResult result = product.adjustPrice(1);

        PriceAdjustedEvent event = result.event();
        assertThat(event.productId()).isEqualTo(1L);
        assertThat(event.sellerId()).isEqualTo(1L);
        assertThat(event.newPrice()).isEqualByComparingTo(new BigDecimal("95.00"));
        assertThat(event.priceLevel()).isEqualTo(1);
    }
}
