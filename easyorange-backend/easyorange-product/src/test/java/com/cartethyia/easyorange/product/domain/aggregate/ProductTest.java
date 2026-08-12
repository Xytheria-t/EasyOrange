package com.cartethyia.easyorange.product.domain.aggregate;

import static org.assertj.core.api.Assertions.*;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
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
import com.cartethyia.easyorange.product.domain.valueobject.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    @DisplayName("创建商品时应生成 ProductCreatedEvent")
    void create_shouldEmitProductCreatedEvent() {
        var t = Product.create(ProductTestFixture.defaultCreateSpec());

        assertThat(t.event()).isInstanceOf(ProductCreatedEvent.class);
    }

    @Test
    @DisplayName("创建商品时名称不能为空")
    void create_withNullTitle_shouldThrow() {
        assertThatThrownBy(() -> Product.create(
                        ProductTestFixture.aProduct().withNoTitle().build()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("创建商品时价格必须大于0")
    void create_withZeroPrice_shouldThrow() {
        assertThatThrownBy(() -> Product.create(
                        ProductTestFixture.aProduct().price(BigDecimal.ZERO).build()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("创建商品时图片不能为空")
    void create_withEmptyImages_shouldThrow() {
        assertThatThrownBy(() -> Product.create(
                        ProductTestFixture.aProduct().emptyImages().build()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("创建商品时应保留图片")
    void create_shouldKeepImages() {
        var p = Product.create(ProductTestFixture.defaultCreateSpec()).aggregate();

        assertThat(p.getImages().isEmpty()).isFalse();
    }

    @Test
    @DisplayName("库存不足时应抛出 InsufficientStockException")
    void decrementStock_whenNoStock_shouldThrow() {
        var p = Product.create(ProductTestFixture.aProduct().stock(0).build()).aggregate();

        assertThatThrownBy(p::decrementStock).isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("扣减库存成功时应减少库存并发布事件")
    void decrementStock_shouldDecreaseAndEmitEvent() {
        var p = ProductTestFixture.defaultProduct();

        var t = p.decrementStock();

        assertThat(t.aggregate().getStock().value()).isEqualTo(9);
        assertThat(t.event()).isInstanceOf(StockDecreasedEvent.class);
    }

    @Test
    @DisplayName("恢复库存成功时应增加库存并发布事件")
    void restoreStock_shouldIncreaseAndEmitEvent() {
        var p = ProductTestFixture.defaultProduct();

        var t = p.restoreStock();

        assertThat(t.aggregate().getStock().value()).isEqualTo(11);
        assertThat(t.event()).isInstanceOf(StockRestoredEvent.class);
    }

    @Test
    @DisplayName("标记售出成功时应更改状态并发布事件")
    void markAsSold_shouldChangeStatusAndEmitEvent() {
        var p = ProductTestFixture.onlineProduct();

        var t = p.markAsSold().orElseThrow();

        assertThat(t.aggregate().getStatus()).isEqualTo(ProductStatus.SOLD);
        assertThat(t.event()).isInstanceOf(ProductMarkedSoldEvent.class);
    }

    @Test
    @DisplayName("非上架商品不能标记为已售")
    void markAsSold_whenNotOnline_shouldThrow() {
        var p = ProductTestFixture.defaultProduct();

        assertThatThrownBy(p::markAsSold).isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("库存为0时不能上架")
    void putOnline_whenNoStock_shouldThrow() {
        var p = Product.create(ProductTestFixture.aProduct().stock(0).build()).aggregate();

        assertThatThrownBy(p::putOnline).isInstanceOf(BusinessException.class).hasMessageContaining("库存不足");
    }

    @Test
    @DisplayName("已售商品重复标记已售应幂等返回空")
    void markAsSold_whenAlreadySold_shouldReturnEmpty() {
        var p = ProductTestFixture.onlineProduct();
        var sold = p.markAsSold().orElseThrow();

        assertThat(sold.aggregate().markAsSold()).isEmpty();
    }

    @Test
    @DisplayName("更新商品信息应修改对应字段")
    void update_shouldModifyFields() {
        var p = ProductTestFixture.defaultProduct();

        var t = p.update(updateWith(CategoryId.of("99"), ProductTitle.of("新名称"), Money.of(new BigDecimal("200"))));

        assertThat(t.aggregate().getCategoryId().value()).isEqualTo("99");
        assertThat(t.aggregate().getTitle().value()).isEqualTo("新名称");
        assertThat(t.aggregate().getPrice().value()).isEqualByComparingTo(new BigDecimal("200"));
    }

    // ==================== submitForReview ====================

    @Test
    @DisplayName("提交审核成功时应变更状态为 PENDING_REVIEW")
    void submitForReview_shouldChangeStatus() {
        var p = ProductTestFixture.defaultProduct();

        var t = p.submitForReview("1");

        assertThat(t.aggregate().getStatus()).isEqualTo(ProductStatus.PENDING_REVIEW);
        assertThat(t.event()).isInstanceOf(ProductSubmittedForReviewEvent.class);
    }

    @Test
    @DisplayName("非资产方不能提交审核")
    void submitForReview_notOwner_shouldThrow() {
        var p = ProductTestFixture.defaultProduct();

        assertThatThrownBy(() -> p.submitForReview("999")).isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("ONLINE 状态的商品不能提交审核")
    void submitForReview_whenOnline_shouldThrow() {
        var p = ProductTestFixture.onlineProduct();

        assertThatThrownBy(() -> p.submitForReview("1")).isInstanceOf(InvalidProductStatusException.class);
    }

    // ==================== approve ====================

    @Test
    @DisplayName("审核通过成功时应变更状态为 ONLINE")
    void approve_shouldChangeStatus() {
        var p = ProductTestFixture.defaultProduct();
        var submitted = p.submitForReview("1").aggregate();

        var t = submitted.approve("审核通过");

        assertThat(t.aggregate().getStatus()).isEqualTo(ProductStatus.ONLINE);
        assertThat(t.event()).isInstanceOf(ProductAuditedEvent.class);
    }

    @Test
    @DisplayName("ONLINE 状态的商品不能审核通过")
    void approve_whenOnline_shouldThrow() {
        var p = ProductTestFixture.onlineProduct();

        assertThatThrownBy(() -> p.approve("审核通过")).isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("REJECTED 状态的商品不能审核通过")
    void approve_whenRejected_shouldThrow() {
        var p = ProductTestFixture.defaultProduct();
        var submitted = p.submitForReview("1").aggregate();
        var rejected = submitted.reject("不合规").aggregate();

        assertThatThrownBy(() -> rejected.approve("审核通过")).isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("信息不完整的商品审核通过应被拦截（与 putOnline 同一组上架不变量）")
    void approve_whenIncomplete_shouldThrow() {
        var p = ProductTestFixture.defaultProduct();
        var submitted = p.submitForReview("1").aggregate();
        var incomplete = submitted.toBuilder().title(null).build();

        assertThatThrownBy(() -> incomplete.approve("审核通过"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("信息不完整");
    }

    // ==================== reject ====================

    @Test
    @DisplayName("审核拒绝成功时应变更状态为 REJECTED")
    void reject_shouldChangeStatus() {
        var p = ProductTestFixture.defaultProduct();
        var submitted = p.submitForReview("1").aggregate();

        var t = submitted.reject("描述不合规");

        assertThat(t.aggregate().getStatus()).isEqualTo(ProductStatus.REJECTED);
        assertThat(t.event()).isInstanceOf(ProductAuditedEvent.class);
    }

    // ==================== putOnline (admin bypass) ====================

    @Test
    @DisplayName("管理员直接上架成功时应变更状态为 ONLINE")
    void putOnline_shouldChangeStatus() {
        var p = ProductTestFixture.defaultProduct();

        var t = p.putOnline();

        assertThat(t.aggregate().getStatus()).isEqualTo(ProductStatus.ONLINE);
        assertThat(t.event()).isInstanceOf(ProductPutOnlineEvent.class);
    }

    @Test
    @DisplayName("上架验证：缺标题时不能上架")
    void putOnline_whenMissingTitle_shouldThrow() {
        var p = ProductTestFixture.defaultProduct();
        var incomplete = p.toBuilder().title(null).build();

        assertThatThrownBy(incomplete::putOnline)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("信息不完整");
    }

    @Test
    @DisplayName("上架验证：缺成色时不能上架")
    void putOnline_whenMissingCondition_shouldThrow() {
        var p = ProductTestFixture.defaultProduct();
        var incomplete = p.toBuilder().conditionLevel(null).build();

        assertThatThrownBy(incomplete::putOnline)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("信息不完整");
    }

    @Test
    @DisplayName("上架验证：零价格时不能上架")
    void putOnline_withZeroPrice_shouldThrow() {
        var p = ProductTestFixture.defaultProduct();
        var zeroPrice = p.toBuilder().price(Money.ZERO).build();

        assertThatThrownBy(zeroPrice::putOnline)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("价格无效");
    }

    @Test
    @DisplayName("已上架的商品不能重复上架")
    void putOnline_whenAlreadyOnline_shouldThrow() {
        var p = ProductTestFixture.onlineProduct();

        assertThatThrownBy(p::putOnline).isInstanceOf(InvalidProductStatusException.class);
    }

    // ==================== takeOffline ====================

    @Test
    @DisplayName("下架成功时应变更状态为 OFFLINE")
    void takeOffline_shouldChangeStatus() {
        var p = ProductTestFixture.onlineProduct();

        var t = p.takeOffline();

        assertThat(t.aggregate().getStatus()).isEqualTo(ProductStatus.OFFLINE);
        assertThat(t.event()).isInstanceOf(ProductTakeOfflineEvent.class);
    }

    @Test
    @DisplayName("DRAFT 状态的商品不能下架")
    void takeOffline_whenDraft_shouldThrow() {
        var p = ProductTestFixture.defaultProduct();

        assertThatThrownBy(p::takeOffline).isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("非资产方不能下架商品")
    void takeOffline_notOwner_shouldThrow() {
        var p = ProductTestFixture.onlineProduct();

        assertThatThrownBy(() -> p.takeOffline("999")).isInstanceOf(InvalidProductStatusException.class);
    }

    // ==================== delete ====================

    @Test
    @DisplayName("删除商品成功时应发布事件")
    void delete_shouldEmitEvent() {
        var p = ProductTestFixture.defaultProduct();

        var t = p.delete("1");

        assertThat(t.event()).isInstanceOf(ProductDeletedEvent.class);
    }

    @Test
    @DisplayName("非资产方不能删除商品")
    void delete_notOwner_shouldThrow() {
        var p = ProductTestFixture.defaultProduct();

        assertThatThrownBy(() -> p.delete("999")).isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("已售商品不能删除")
    void delete_whenSold_shouldThrow() {
        var p = ProductTestFixture.onlineProduct();
        var sold = p.markAsSold().orElseThrow().aggregate();

        assertThatThrownBy(() -> sold.delete("1")).isInstanceOf(InvalidProductStatusException.class);
    }

    // ==================== restoreStock edge cases ====================

    @Test
    @DisplayName("已售商品不能恢复库存")
    void restoreStock_whenSold_shouldThrow() {
        var p = ProductTestFixture.onlineProduct();
        var sold = p.markAsSold().orElseThrow().aggregate();

        assertThatThrownBy(sold::restoreStock).isInstanceOf(InvalidProductStatusException.class);
    }

    @Test
    @DisplayName("下架商品不能恢复库存")
    void restoreStock_whenOffline_shouldThrow() {
        var p = ProductTestFixture.onlineProduct();
        var offline = p.takeOffline().aggregate();

        assertThatThrownBy(offline::restoreStock).isInstanceOf(InvalidProductStatusException.class);
    }

    // ==================== predicates ====================

    @Test
    @DisplayName("完整商品信息应通过 isComplete 校验")
    void isComplete_withFullInfo_shouldReturnTrue() {
        var p = ProductTestFixture.defaultProduct();
        assertThat(p.isComplete()).isTrue();
    }

    @Test
    @DisplayName("缺价格时 isComplete 应为 false")
    void isComplete_withoutPrice_shouldReturnFalse() {
        var p = ProductTestFixture.defaultProduct();
        var incomplete = p.toBuilder().price(null).build();
        assertThat(incomplete.isComplete()).isFalse();
    }

    @Test
    @DisplayName("缺标题时 isComplete 应为 false")
    void isComplete_withoutTitle_shouldReturnFalse() {
        var p = ProductTestFixture.defaultProduct();
        var incomplete = p.toBuilder().title(null).build();
        assertThat(incomplete.isComplete()).isFalse();
    }

    @Test
    @DisplayName("缺成色时 isComplete 应为 false")
    void isComplete_withoutCondition_shouldReturnFalse() {
        var p = ProductTestFixture.defaultProduct();
        var incomplete = p.toBuilder().conditionLevel(null).build();
        assertThat(incomplete.isComplete()).isFalse();
    }

    @Test
    @DisplayName("hasValidPrice 在价格为0时应返回 false")
    void hasValidPrice_withZero_shouldReturnFalse() {
        var p = ProductTestFixture.defaultProduct();
        var zeroPrice = p.toBuilder().price(Money.ZERO).build();
        assertThat(zeroPrice.hasValidPrice()).isFalse();
    }

    @Test
    @DisplayName("hasStock 应正确判断库存")
    void hasStock_shouldReturnCorrectValue() {
        var p = ProductTestFixture.defaultProduct();
        assertThat(p.hasStock()).isTrue();

        var noStock = p.toBuilder().stock(StockQuantity.of(0)).build();
        assertThat(noStock.hasStock()).isFalse();
    }

    // ==================== assignId ====================

    @Test
    @DisplayName("assignId 应设置聚合根 ID")
    void assignId_shouldSetId() {
        var p = Product.create(ProductTestFixture.defaultCreateSpec()).aggregate();

        var assigned = p.assignId("test-id");

        assertThat(assigned.getId().value()).isEqualTo("test-id");
    }

    @Test
    @DisplayName("已分配 ID 时 assignId 不应覆盖")
    void assignId_whenAlreadySet_shouldNotOverride() {
        var p = ProductTestFixture.defaultProduct();
        assertThat(p.getId().value()).isEqualTo("1");

        var overridden = p.assignId("other-id");
        assertThat(overridden.getId().value()).isEqualTo("1");
    }

    private static ProductUpdateSpec updateWith(CategoryId categoryId, ProductTitle title, Money price) {
        return new ProductUpdateSpec(categoryId, title, price, null, null, null, null, null, null, null);
    }
}
