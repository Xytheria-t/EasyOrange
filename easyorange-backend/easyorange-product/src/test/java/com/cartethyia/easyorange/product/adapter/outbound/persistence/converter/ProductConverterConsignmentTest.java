package com.cartethyia.easyorange.product.adapter.outbound.persistence.converter;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ConsignmentMode;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.valueobject.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ProductConverter consignment field mappings.
 * <p>
 * Verifies bidirectional mapping of floorPrice, consignmentMode, listedAt, and currentPriceLevel
 * between ProductDO and Product domain objects.
 */
class ProductConverterConsignmentTest {

    private final ProductConverter converter = new ProductConverter();

    // ==================== toDomain() tests ====================

    @Test
    @DisplayName("toDomain: floorPrice=null, consignmentMode=0 => domain.floorPrice=null, consignmentMode=MANUAL")
    void toDomain_withoutConsignmentFields_shouldMapToDefaults() {
        ProductDO productDO = createBasicProductDO();
        productDO.setFloorPrice(null);
        productDO.setConsignmentMode(0);
        productDO.setListedAt(null);
        productDO.setCurrentPriceLevel(0);

        Product domain = converter.toDomain(productDO, null, List.of());

        assertThat(domain.getFloorPrice()).isNull();
        assertThat(domain.getConsignmentMode()).isEqualTo(ConsignmentMode.MANUAL);
        assertThat(domain.getListedAt()).isNull();
        assertThat(domain.getCurrentPriceLevel()).isEqualTo(0);
    }

    @Test
    @DisplayName("toDomain: floorPrice=50.00, consignmentMode=1, listedAt set, currentPriceLevel=2 => correct domain fields")
    void toDomain_withConsignmentFields_shouldMapCorrectly() {
        ProductDO productDO = createBasicProductDO();
        productDO.setFloorPrice(new BigDecimal("50.00"));
        productDO.setConsignmentMode(1);
        LocalDateTime now = LocalDateTime.now();
        productDO.setListedAt(now);
        productDO.setCurrentPriceLevel(2);

        Product domain = converter.toDomain(productDO, null, List.of());

        assertThat(domain.getFloorPrice()).isNotNull();
        assertThat(domain.getFloorPrice().value()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(domain.getConsignmentMode()).isEqualTo(ConsignmentMode.AI_MANAGED);
        assertThat(domain.getListedAt()).isEqualTo(now);
        assertThat(domain.getCurrentPriceLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("toDomain: consignmentMode=99 (unknown) => falls back to MANUAL")
    void toDomain_withUnknownConsignmentMode_shouldFallbackToManual() {
        ProductDO productDO = createBasicProductDO();
        productDO.setConsignmentMode(99);

        Product domain = converter.toDomain(productDO, null, List.of());

        assertThat(domain.getConsignmentMode()).isEqualTo(ConsignmentMode.MANUAL);
    }

    // ==================== toDO() tests ====================

    @Test
    @DisplayName("toDO: AI_MANAGED mode with all consignment fields => DO fields correct")
    void toDO_withAiManagedFields_shouldMapCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        Product domain = Product.builder()
                .id(ProductId.of(1L))
                .sellerId(SellerId.of(10L))
                .categoryId(CategoryId.of(2L))
                .title(ProductTitle.of("测试商品"))
                .price(Money.of(new BigDecimal("100.00")))
                .originalPrice(null)
                .floorPrice(Money.of(new BigDecimal("50.00")))
                .consignmentMode(ConsignmentMode.AI_MANAGED)
                .listedAt(now)
                .currentPriceLevel(2)
                .stock(StockQuantity.of(5))
                .version(Version.INITIAL)
                .status(ProductStatus.ONLINE)
                .viewCount(0)
                .conditionLevel(ConditionLevel.NEW)
                .location(null)
                .contactMethod(null)
                .description(null)
                .images(ImageSet.empty())
                .tags(TagSet.empty())
                .searchText(null)
                .priceUpdateTime(null)
                .createTime(now)
                .updateTime(now)
                .build();

        ProductDO productDO = converter.toDataObject(domain);

        assertThat(productDO.getFloorPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(productDO.getConsignmentMode()).isEqualTo(1);
        assertThat(productDO.getListedAt()).isEqualTo(now);
        assertThat(productDO.getCurrentPriceLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("toDO: MANUAL mode with null floorPrice => DO fields correct")
    void toDO_withManualMode_shouldMapNullFloorPrice() {
        LocalDateTime now = LocalDateTime.now();
        Product domain = Product.builder()
                .id(ProductId.of(1L))
                .sellerId(SellerId.of(10L))
                .categoryId(CategoryId.of(2L))
                .title(ProductTitle.of("手动商品"))
                .price(Money.of(new BigDecimal("100.00")))
                .originalPrice(null)
                .floorPrice(null)
                .consignmentMode(ConsignmentMode.MANUAL)
                .listedAt(null)
                .currentPriceLevel(0)
                .stock(StockQuantity.of(5))
                .version(Version.INITIAL)
                .status(ProductStatus.DRAFT)
                .viewCount(0)
                .conditionLevel(ConditionLevel.NEW)
                .location(null)
                .contactMethod(null)
                .description(null)
                .images(ImageSet.empty())
                .tags(TagSet.empty())
                .searchText(null)
                .priceUpdateTime(null)
                .createTime(now)
                .updateTime(now)
                .build();

        ProductDO productDO = converter.toDataObject(domain);

        assertThat(productDO.getFloorPrice()).isNull();
        assertThat(productDO.getConsignmentMode()).isEqualTo(0);
        assertThat(productDO.getListedAt()).isNull();
        assertThat(productDO.getCurrentPriceLevel()).isEqualTo(0);
    }

    // ==================== Bidirectional mapping tests ====================

    @Test
    @DisplayName("Bidirectional: DO → Domain → DO should preserve all consignment fields")
    void bidirectionalMapping_shouldPreserveConsignmentFields() {
        // Arrange: create DO with specific consignment values
        LocalDateTime now = LocalDateTime.now();
        ProductDO originalDO = createBasicProductDO();
        originalDO.setFloorPrice(new BigDecimal("75.00"));
        originalDO.setConsignmentMode(1);
        originalDO.setListedAt(now);
        originalDO.setCurrentPriceLevel(3);

        // Act 1: DO → Domain
        Product domain = converter.toDomain(originalDO, null, List.of());

        // Assert intermediate domain state
        assertThat(domain.getFloorPrice().value()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(domain.getConsignmentMode()).isEqualTo(ConsignmentMode.AI_MANAGED);

        // Act 2: Domain → DO
        ProductDO resultDO = converter.toDataObject(domain);

        // Assert final DO state matches original
        assertThat(resultDO.getFloorPrice()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(resultDO.getConsignmentMode()).isEqualTo(1);
        assertThat(resultDO.getListedAt()).isEqualTo(now);
        assertThat(resultDO.getCurrentPriceLevel()).isEqualTo(3);
    }

    // ==================== Helper methods ====================

    private ProductDO createBasicProductDO() {
        ProductDO productDO = new ProductDO();
        productDO.setId(1L);
        productDO.setUserId(10L);
        productDO.setCategoryId(2L);
        productDO.setName("测试商品");
        productDO.setPrice(new BigDecimal("100.00"));
        productDO.setOriginalPrice(null);
        productDO.setStock(5);
        productDO.setVersion(0);
        productDO.setStatus(1);
        productDO.setViewCount(10);
        productDO.setConditionLevel(1);
        productDO.setLocation("北京");
        productDO.setContactMethod("微信");
        productDO.setTags("");
        productDO.setSearchText(null);
        productDO.setPriceUpdateTime(LocalDateTime.now());
        productDO.setCreateTime(LocalDateTime.now());
        productDO.setUpdateTime(LocalDateTime.now());
        return productDO;
    }
}
