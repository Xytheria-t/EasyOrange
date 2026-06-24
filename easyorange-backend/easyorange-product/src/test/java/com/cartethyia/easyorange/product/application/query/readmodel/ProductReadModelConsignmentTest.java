package com.cartethyia.easyorange.product.application.query.readmodel;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.application.query.assembler.ProductReadModelAssembler;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductCreatedResult;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ConsignmentMode;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("商品寄售读模型映射测试")
class ProductReadModelConsignmentTest {

    private ProductReadModelAssembler assembler;
    private Product aiManagedProduct;
    private Product manualProduct;
    private ProductReadModel aiManagedReadModel;
    private ProductReadModel manualReadModel;

    @BeforeEach
    void setUp() {
        assembler = new ProductReadModelAssembler();

        // Create AI_MANAGED product with floorPrice
        ProductCreatedResult aiResult = Product.create(
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
                ProductDescription.of("AI托管商品描述"),
                ImageSet.of(List.of("http://img/1.jpg"))
        );
        aiManagedProduct = aiResult.product().assignId(1L);

        // Create MANUAL product without floorPrice
        ProductCreatedResult manualResult = Product.create(
                SellerId.of(1L),
                CategoryId.of(2L),
                ProductTitle.of("手动商品"),
                Money.of(new BigDecimal("200")),
                null,
                null,
                ConsignmentMode.MANUAL,
                StockQuantity.of(5),
                ConditionLevel.LIKE_NEW,
                TradeLocation.of("上海"),
                ContactMethod.of("QQ"),
                ProductDescription.of("手动商品描述"),
                ImageSet.of(List.of("http://img/2.jpg"))
        );
        manualProduct = manualResult.product().assignId(2L);

        // Set up read model for AI_MANAGED
        aiManagedReadModel = new ProductReadModel(
                1L, 1L, "seller", null, 2L, "分类",
                "AI托管商品", "AI托管商品描述",
                new BigDecimal("100"), null,
                10, 1, "上架", 0, 1, "全新",
                "北京", "微信", List.of("http://img/1.jpg"), "http://img/1.jpg",
                new BigDecimal("50"), 1, null, 2,
                null, null
        );

        // Set up read model for MANUAL
        manualReadModel = new ProductReadModel(
                2L, 1L, "seller", null, 2L, "分类",
                "手动商品", "手动商品描述",
                new BigDecimal("200"), null,
                5, 1, "上架", 0, 2, "良好",
                "上海", "QQ", List.of("http://img/2.jpg"), "http://img/2.jpg",
                null, 0, null, 0,
                null, null
        );
    }

    // ==================== toProductVO(Product domain) tests ====================

    @Test
    @DisplayName("AI托管商品 from domain: floorPrice/consignmentMode/currentPriceLevel 应正确映射")
    void toProductVO_fromDomain_aiManaged_shouldMapConsignmentFields() {
        ProductVO vo = assembler.toProductVO(aiManagedProduct, Map.of(), Map.of(), Map.of(), Map.of());

        assertThat(vo.getFloorPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(vo.getConsignmentMode()).isEqualTo(1);
        assertThat(vo.getCurrentPriceLevel()).isEqualTo(0);
    }

    @Test
    @DisplayName("手动商品 from domain: floorPrice 应为 null, consignmentMode 为 0")
    void toProductVO_fromDomain_manual_shouldHaveNullFloorPrice() {
        ProductVO vo = assembler.toProductVO(manualProduct, Map.of(), Map.of(), Map.of(), Map.of());

        assertThat(vo.getFloorPrice()).isNull();
        assertThat(vo.getConsignmentMode()).isEqualTo(0);
        assertThat(vo.getCurrentPriceLevel()).isEqualTo(0);
    }

    @Test
    @DisplayName("AI托管商品上架后 from domain: currentPriceLevel 应更新")
    void toProductVO_fromDomain_aiManagedAfterOnline_shouldReflectPriceLevel() {
        Product onlineProduct = aiManagedProduct.putOnline();
        ProductVO vo = assembler.toProductVO(onlineProduct, Map.of(), Map.of(), Map.of(), Map.of());

        assertThat(vo.getFloorPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(vo.getConsignmentMode()).isEqualTo(1);
        assertThat(vo.getCurrentPriceLevel()).isEqualTo(0);
    }

    // ==================== toProductVO(ProductReadModel) tests ====================

    @Test
    @DisplayName("AI托管商品 from readModel: floorPrice/consignmentMode/currentPriceLevel 应正确映射")
    void toProductVO_fromReadModel_aiManaged_shouldMapConsignmentFields() {
        ProductVO vo = assembler.toProductVO(aiManagedReadModel);

        assertThat(vo.getFloorPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(vo.getConsignmentMode()).isEqualTo(1);
        assertThat(vo.getCurrentPriceLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("手动商品 from readModel: floorPrice 应为 null, consignmentMode 为 0")
    void toProductVO_fromReadModel_manual_shouldHaveNullFloorPrice() {
        ProductVO vo = assembler.toProductVO(manualReadModel);

        assertThat(vo.getFloorPrice()).isNull();
        assertThat(vo.getConsignmentMode()).isEqualTo(0);
        assertThat(vo.getCurrentPriceLevel()).isEqualTo(0);
    }

    // ==================== ProductReadModel record field tests ====================

    @Test
    @DisplayName("AI托管商品 ProductReadModel 应包含所有寄售字段")
    void readModel_aiManaged_shouldContainConsignmentFields() {
        assertThat(aiManagedReadModel.floorPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(aiManagedReadModel.consignmentMode()).isEqualTo(1);
        assertThat(aiManagedReadModel.listedAt()).isNull();
        assertThat(aiManagedReadModel.currentPriceLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("手动商品 ProductReadModel 的寄售字段应为 null/0")
    void readModel_manual_shouldHaveDefaultConsignmentFields() {
        assertThat(manualReadModel.floorPrice()).isNull();
        assertThat(manualReadModel.consignmentMode()).isEqualTo(0);
        assertThat(manualReadModel.listedAt()).isNull();
        assertThat(manualReadModel.currentPriceLevel()).isEqualTo(0);
    }

    @Test
    @DisplayName("ProductVO builder 应支持链式设置新字段")
    void productVO_builder_shouldSupportConsignmentFields() {
        ProductVO vo = ProductVO.builder()
                .id(1L)
                .title("测试")
                .price(new BigDecimal("100"))
                .floorPrice(new BigDecimal("50"))
                .consignmentMode(1)
                .currentPriceLevel(2)
                .build();

        assertThat(vo.getFloorPrice()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(vo.getConsignmentMode()).isEqualTo(1);
        assertThat(vo.getCurrentPriceLevel()).isEqualTo(2);
    }
}
