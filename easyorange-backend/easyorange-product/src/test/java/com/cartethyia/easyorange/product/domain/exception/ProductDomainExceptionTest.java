package com.cartethyia.easyorange.product.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 具名工厂的 null 入参契约 —— 每个工厂的消息都带定位信息，null 分支退化（把 null 拼成 "null"
 * 字符串的分支被删）会让排障信息静默丢失；调用方拿不到对象时不该在拼消息时二次 NPE。
 */
class ProductDomainExceptionTest {

    @Test
    @DisplayName("id 为 null 时消息带 null 字面量且码值正确（不 NPE）")
    void notFound_withNullId_shouldNotThrow() {
        ProductDomainException byVo = ProductDomainException.notFound((ProductId) null);
        ProductDomainException byStr = ProductDomainException.notFound((String) null);

        assertThat(byVo.getCode()).isEqualTo(ProductResultCode.PRODUCT_NOT_FOUND.getCode());
        assertThat(byVo.getMessage()).contains("id=null");
        assertThat(byStr.getCode()).isEqualTo(ProductResultCode.PRODUCT_NOT_FOUND.getCode());
        assertThat(byStr.getMessage()).contains("id=null");
    }

    @Test
    @DisplayName("id 非 null 时消息带 id 原文")
    void notFound_withId_shouldCarryId() {
        assertThat(ProductDomainException.notFound(ProductId.of("p-1")).getMessage())
                .contains("id=p-1");
        assertThat(ProductDomainException.notFound("p-2").getMessage()).contains("id=p-2");
    }

    @Test
    @DisplayName("notOwner / invalidStatus / insufficientStock 的 productId 与上下文字段 null 时不 NPE")
    void contextFactories_withNulls_shouldNotThrow() {
        var notOwner = ProductDomainException.notOwner(null, "试试");
        var invalidStatus = ProductDomainException.invalidStatus("状态不对", null, null);
        var insufficientStock = ProductDomainException.insufficientStock("库存不够", null, null);

        assertThat(notOwner.getCode()).isEqualTo(ProductResultCode.PRODUCT_NOT_OWNER.getCode());
        assertThat(notOwner.getMessage()).contains("productId=null");
        assertThat(invalidStatus.getCode()).isEqualTo(ProductResultCode.PRODUCT_STATUS_INVALID.getCode());
        assertThat(invalidStatus.getMessage()).contains("productId=null", "currentStatus=null");
        assertThat(insufficientStock.getCode()).isEqualTo(ProductResultCode.PRODUCT_OUT_OF_STOCK.getCode());
        assertThat(insufficientStock.getMessage()).contains("stock=null");
    }

    @Test
    @DisplayName("上下文非 null 时消息带实际值")
    void contextFactories_withValues_shouldCarryThem() {
        var productId = ProductId.of("p-1");

        assertThat(ProductDomainException.notOwner(productId, "试试").getMessage())
                .contains("productId=p-1");
        assertThat(ProductDomainException.invalidStatus("状态不对", productId, ProductStatus.SOLD)
                        .getMessage())
                .contains("productId=p-1", "currentStatus=SOLD");
        assertThat(ProductDomainException.insufficientStock("库存不够", productId, StockQuantity.of(3))
                        .getMessage())
                .contains("stock=3");
    }

    @Test
    @DisplayName("分类系工厂的 id / parentId 为 null 时消息带 <root> 或 null 且码值正确")
    void categoryFactories_withNulls_shouldNotThrow() {
        assertThat(ProductDomainException.categoryNotFound(null).getCode())
                .isEqualTo(ProductResultCode.CATEGORY_NOT_FOUND.getCode());
        assertThat(ProductDomainException.categoryNotFound(null).getMessage()).contains("id=null");
        assertThat(ProductDomainException.categoryParentNotFound(null).getCode())
                .isEqualTo(ProductResultCode.CATEGORY_PARENT_NOT_FOUND.getCode());
        assertThat(ProductDomainException.categoryDuplicatedName(null, null).getMessage())
                .contains("parentId=<root>");
        assertThat(ProductDomainException.categoryCycleDetected(null, null).getCode())
                .isEqualTo(ProductResultCode.CATEGORY_CYCLE_DETECTED.getCode());
    }

    @Test
    @DisplayName("分类系工厂非 null 时消息带实际值")
    void categoryFactories_withValues_shouldCarryThem() {
        assertThat(ProductDomainException.categoryDuplicatedName("同名", "p-1").getMessage())
                .contains("name=同名", "parentId=p-1");
    }

    @Test
    @DisplayName("层级越界消息带实际上限与实际层级")
    void categoryLevelExceeded_shouldCarryBothLevels() {
        assertThat(ProductDomainException.categoryLevelExceeded(4, 3).getMessage())
                .contains("不能超过3级", "实际 4 级");
    }

    @Test
    @DisplayName("所有工厂产出的异常都是 BaseBusinessException 子类（Rule 11：唯一领域异常根）")
    void allFactories_shouldProduceBaseBusinessException() {
        assertThat(ProductDomainException.notFound("x")).isInstanceOf(BaseBusinessException.class);
    }
}
