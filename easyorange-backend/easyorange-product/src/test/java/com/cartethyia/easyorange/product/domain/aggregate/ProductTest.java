package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.domain.event.ProductAuditedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductDeletedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductMarkedSoldEvent;
import com.cartethyia.easyorange.product.domain.event.ProductPutOnlineEvent;
import com.cartethyia.easyorange.product.domain.event.ProductSubmittedForReviewEvent;
import com.cartethyia.easyorange.product.domain.event.ProductTakeOfflineEvent;
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

    // ==================== submitForReview ====================

    @Test
    @DisplayName("提交审核成功时应变更状态为 PENDING_REVIEW")
    void submitForReview_shouldChangeStatus() {
        var product = ProductTestFixture.defaultProduct();

        var result = product.submitForReview("1");

        assertThat(result.product().getStatus()).isEqualTo(ProductStatus.PENDING_REVIEW);
        assertThat(result.event()).isInstanceOf(ProductSubmittedForReviewEvent.class);
    }

    @Test
    @DisplayName("非资产方不能提交审核")
    void submitForReview_notOwner_shouldThrow() {
        var product = ProductTestFixture.defaultProduct();

        assertThatThrownBy(() -> product.submitForReview("999"))
                .isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("ONLINE 状态的商品不能提交审核")
    void submitForReview_whenOnline_shouldThrow() {
        var product = ProductTestFixture.defaultProduct().putOnline().product();

        assertThatThrownBy(() -> product.submitForReview("1"))
                .isInstanceOf(InvalidProductStatusException.class);
    }

    // ==================== approve ====================

    @Test
    @DisplayName("审核通过成功时应变更状态为 ONLINE")
    void approve_shouldChangeStatus() {
        var product = ProductTestFixture.defaultProduct();
        product = product.submitForReview("1").product();

        var result = product.approve("审核通过");

        assertThat(result.product().getStatus()).isEqualTo(ProductStatus.ONLINE);
        assertThat(result.event()).isInstanceOf(ProductAuditedEvent.class);
    }

    @Test
    @DisplayName("ONLINE 状态的商品不能审核通过")
    void approve_whenOnline_shouldThrow() {
        var published = ProductTestFixture.defaultProduct().putOnline().product();

        assertThatThrownBy(() -> published.approve("审核通过"))
                .isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("REJECTED 状态的商品不能审核通过")
    void approve_whenRejected_shouldThrow() {
        var submitted = ProductTestFixture.defaultProduct().submitForReview("1").product();
        var rejected = submitted.reject("不合规").product();

        assertThatThrownBy(() -> rejected.approve("审核通过"))
                .isInstanceOf(InvalidProductStatusException.class);
    }

    // ==================== reject ====================

    @Test
    @DisplayName("审核拒绝成功时应变更状态为 REJECTED")
    void reject_shouldChangeStatus() {
        var product = ProductTestFixture.defaultProduct();
        product = product.submitForReview("1").product();

        var result = product.reject("描述不合规");

        assertThat(result.product().getStatus()).isEqualTo(ProductStatus.REJECTED);
        assertThat(result.event()).isInstanceOf(ProductAuditedEvent.class);
    }

    // ==================== putOnline (admin bypass) ====================

    @Test
    @DisplayName("管理员直接上架成功时应变更状态为 ONLINE")
    void putOnline_shouldChangeStatus() {
        var product = ProductTestFixture.defaultProduct();

        var result = product.putOnline();

        assertThat(result.product().getStatus()).isEqualTo(ProductStatus.ONLINE);
        assertThat(result.event()).isInstanceOf(ProductPutOnlineEvent.class);
    }

    @Test
    @DisplayName("上架验证：缺标题时不能上架")
    void putOnline_whenMissingTitle_shouldThrow() {
        var product = ProductTestFixture.defaultProduct();
        var incomplete = product.toBuilder().title(null).build();

        assertThatThrownBy(incomplete::putOnline)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("信息不完整");
    }

    @Test
    @DisplayName("上架验证：缺成色时不能上架")
    void putOnline_whenMissingCondition_shouldThrow() {
        var product = ProductTestFixture.defaultProduct();
        var incomplete = product.toBuilder().conditionLevel(null).build();

        assertThatThrownBy(incomplete::putOnline)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("信息不完整");
    }

    @Test
    @DisplayName("上架验证：零价格时不能上架")
    void putOnline_withZeroPrice_shouldThrow() {
        var product = ProductTestFixture.defaultProduct();
        var zeroPrice = product.toBuilder().price(Money.ZERO).build();

        assertThatThrownBy(zeroPrice::putOnline)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("价格无效");
    }

    @Test
    @DisplayName("已上架的商品不能重复上架")
    void putOnline_whenAlreadyOnline_shouldThrow() {
        var product = ProductTestFixture.defaultProduct().putOnline().product();

        assertThatThrownBy(product::putOnline)
                .isInstanceOf(InvalidProductStatusException.class);
    }

    // ==================== takeOffline ====================

    @Test
    @DisplayName("下架成功时应变更状态为 OFFLINE")
    void takeOffline_shouldChangeStatus() {
        var product = ProductTestFixture.defaultProduct().putOnline().product();

        var result = product.takeOffline();

        assertThat(result.product().getStatus()).isEqualTo(ProductStatus.OFFLINE);
        assertThat(result.event()).isInstanceOf(ProductTakeOfflineEvent.class);
    }

    @Test
    @DisplayName("DRAFT 状态的商品不能下架")
    void takeOffline_whenDraft_shouldThrow() {
        var product = ProductTestFixture.defaultProduct();

        assertThatThrownBy(product::takeOffline)
                .isInstanceOf(InvalidProductStatusException.class);
    }

    // ==================== delete ====================

    @Test
    @DisplayName("删除商品成功时应发布事件")
    void delete_shouldEmitEvent() {
        var product = ProductTestFixture.defaultProduct().assignId("1");

        var result = product.delete("1");

        assertThat(result.event()).isInstanceOf(ProductDeletedEvent.class);
    }

    @Test
    @DisplayName("非资产方不能删除商品")
    void delete_notOwner_shouldThrow() {
        var product = ProductTestFixture.defaultProduct().assignId("1");

        assertThatThrownBy(() -> product.delete("999"))
                .isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("已售商品不能删除")
    void delete_whenSold_shouldThrow() {
        var online = ProductTestFixture.defaultProduct().putOnline().product();
        var sold = online.markAsSold().product();

        assertThatThrownBy(() -> sold.delete("1"))
                .isInstanceOf(InvalidProductStatusException.class);
    }

    // ==================== restoreStock edge cases ====================

    @Test
    @DisplayName("已售商品不能恢复库存")
    void restoreStock_whenSold_shouldThrow() {
        var product = ProductTestFixture.defaultProduct();
        product = product.putOnline().product();
        product = product.markAsSold().product();

        assertThatThrownBy(product::restoreStock)
                .isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("下架商品不能恢复库存")
    void restoreStock_whenOffline_shouldThrow() {
        var product = ProductTestFixture.defaultProduct();
        product = product.putOnline().product();
        product = product.takeOffline().product();

        assertThatThrownBy(product::restoreStock)
                .isInstanceOf(InvalidProductStatusException.class);
    }

    // ==================== predicates ====================

    @Test
    @DisplayName("完整商品信息应通过 isComplete 校验")
    void isComplete_withFullInfo_shouldReturnTrue() {
        var product = ProductTestFixture.defaultProduct();
        assertThat(product.isComplete()).isTrue();
    }

    @Test
    @DisplayName("缺价格时 isComplete 应为 false")
    void isComplete_withoutPrice_shouldReturnFalse() {
        var product = ProductTestFixture.defaultProduct();
        var incomplete = product.toBuilder().price(null).build();
        assertThat(incomplete.isComplete()).isFalse();
    }

    @Test
    @DisplayName("缺标题时 isComplete 应为 false")
    void isComplete_withoutTitle_shouldReturnFalse() {
        var product = ProductTestFixture.defaultProduct();
        var incomplete = product.toBuilder().title(null).build();
        assertThat(incomplete.isComplete()).isFalse();
    }

    @Test
    @DisplayName("缺成色时 isComplete 应为 false")
    void isComplete_withoutCondition_shouldReturnFalse() {
        var product = ProductTestFixture.defaultProduct();
        var incomplete = product.toBuilder().conditionLevel(null).build();
        assertThat(incomplete.isComplete()).isFalse();
    }

    @Test
    @DisplayName("hasValidPrice 在价格为0时应返回 false")
    void hasValidPrice_withZero_shouldReturnFalse() {
        var product = ProductTestFixture.defaultProduct();
        var zeroPrice = product.toBuilder().price(Money.ZERO).build();
        assertThat(zeroPrice.hasValidPrice()).isFalse();
    }

    @Test
    @DisplayName("hasStock 应正确判断库存")
    void hasStock_shouldReturnCorrectValue() {
        var product = ProductTestFixture.defaultProduct();
        assertThat(product.hasStock()).isTrue();

        var noStock = product.toBuilder().stock(StockQuantity.of(0)).build();
        assertThat(noStock.hasStock()).isFalse();
    }

    // ==================== assignId ====================

    @Test
    @DisplayName("assignId 应设置聚合根 ID")
    void assignId_shouldSetId() {
        var product = Product.create(ProductTestFixture.defaultCreateSpec()).product();

        var result = product.assignId("test-id");

        assertThat(result.getId().value()).isEqualTo("test-id");
    }

    @Test
    @DisplayName("已分配 ID 时 assignId 不应覆盖")
    void assignId_whenAlreadySet_shouldNotOverride() {
        var product = ProductTestFixture.defaultProduct();
        assertThat(product.getId().value()).isEqualTo("1");

        var result = product.assignId("other-id");
        assertThat(result.getId().value()).isEqualTo("1");
    }

    private static ProductUpdateSpec updateWith(CategoryId categoryId, ProductTitle title, Money price) {
        return new ProductUpdateSpec(categoryId, title, price, null, null, null, null, null, null, null);
    }
}
