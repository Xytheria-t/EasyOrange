package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.metrics.BusinessMetricsService;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.ProductCreateSpec;
import com.cartethyia.easyorange.product.domain.aggregate.ProductUpdateSpec;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductResultCode;
import com.cartethyia.easyorange.product.domain.enums.StockChangeType;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductEvent;
import com.cartethyia.easyorange.product.domain.exception.ProductDomainException;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.repository.StockLedgerRepository;
import com.cartethyia.easyorange.product.domain.valueobject.*;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ProductCommandHandler {

    private final ProductRepository productRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final BusinessMetricsService businessMetricsService;

    // ==================== CRUD ====================

    public String createProduct(String userId, CreateProductCommand command) {
        var created = Product.create(new ProductCreateSpec(
                SellerId.of(userId),
                CategoryId.of(command.categoryId()),
                ProductTitle.of(command.name()),
                Money.of(command.price()),
                mapIfPresent(command.originalPrice(), Money::of),
                StockQuantity.of(command.stock() != null ? command.stock() : 1),
                parseConditionLevel(command.conditionLevel()),
                TradeLocation.of(command.location()),
                ContactMethod.of(command.contactMethod()),
                ProductDescription.of(command.description()),
                ImageSet.of(command.imageUrls()),
                command.aiSuggestion()));

        var saved = productRepository.save(created.aggregate());
        // 库存基线落账：与资产创建同事务，给对账任务一个起点
        var stock = saved.getStock().value();
        stockLedgerRepository.record(new StockChange(StockChangeType.INIT, null, saved.getId(), stock, stock));
        // 事件须用落库后的聚合重建：Product.create 阶段尚未分配 ID，直接发布会带 null productId（ES 索引跳过、通知发错对象）
        domainEventPublisher.publish(new ProductCreatedEvent(created.event().eventId(), ProductEvent.Data.from(saved)));
        return saved.getId().value();
    }

    public void updateProduct(String userId, UpdateProductCommand command) {
        var product = findByIdOrThrow(ProductId.of(command.id()));

        var result = product.update(
                userId,
                new ProductUpdateSpec(
                        mapIfPresent(command.categoryId(), CategoryId::of),
                        mapIfPresent(command.name(), ProductTitle::of),
                        mapIfPresent(command.price(), Money::of),
                        mapIfPresent(command.originalPrice(), Money::of),
                        mapIfPresent(command.stock(), StockQuantity::of),
                        mapIfPresent(command.conditionLevel(), this::parseConditionLevel),
                        mapIfPresent(command.location(), TradeLocation::of),
                        mapIfPresent(command.contactMethod(), ContactMethod::of),
                        mapIfPresent(command.description(), ProductDescription::of),
                        mapIfPresent(command.imageUrls(), ImageSet::of)));

        recordManualAdjustment(product, result.aggregate());
        productRepository.save(result.aggregate());
        domainEventPublisher.publish(result.event());
    }

    public void deleteProduct(String userId, String id) {
        var pid = ProductId.of(id);
        var product = findByIdOrThrow(pid);

        var t = product.delete(userId);
        productRepository.delete(pid);
        domainEventPublisher.publish(t.event());
    }

    // ==================== Stock ====================

    /**
     * 下单扣减库存 — 先以 {@code (DECREASE, orderId, productId)} 抢占流水落账权，再改库存并发布事件。
     * <p>
     * 抢不到（该订单已扣过这件资产）说明是重复投递，直接返回：库存与事件都不再重复发生。
     * 落账与库存更新同事务，任一失败整体回滚。
     */
    public void decrementStock(String orderId, String productId, int quantity) {
        var pid = ProductId.of(productId);
        var product = findByIdOrThrow(pid);
        var result = product.decrementStock(quantity);

        if (!claimStockChange(StockChange.decrease(
                orderId, pid, quantity, result.aggregate().getStock().value()))) {
            return;
        }
        productRepository.save(result.aggregate());
        domainEventPublisher.publish(result.event());
    }

    /**
     * 取消 / 退款恢复库存 — 幂等语义同 {@link #decrementStock}：同一订单对同一资产只恢复一次，
     * 恢复数量取下单时扣减的数量（由订单事件携带），不做「无条件 +1」。
     */
    public void restoreStock(String orderId, String productId, int quantity) {
        var pid = ProductId.of(productId);
        var product = findByIdOrThrow(pid);
        var result = product.restoreStock(quantity);

        if (!claimStockChange(StockChange.restore(
                orderId, pid, quantity, result.aggregate().getStock().value()))) {
            return;
        }
        productRepository.save(result.aggregate());
        domainEventPublisher.publish(result.event());
    }

    // ==================== Status Transitions ====================

    public void submitForReview(String userId, String productId) {
        var product = findByIdOrThrow(ProductId.of(productId));
        mutate(product, p -> p.submitForReview(userId));
    }

    public void putOnline(String productId) {
        var product = findByIdOrThrow(ProductId.of(productId));
        mutate(product, Product::putOnline);
    }

    public void takeOffline(String userId, String productId) {
        var product = findByIdOrThrow(ProductId.of(productId));
        mutate(product, p -> p.takeOffline(userId));
    }

    public void markAsSold(String productId) {
        var product = findByIdOrThrow(ProductId.of(productId));
        mutateIfPresent(product, Product::markAsSold);
    }

    // ==================== Private Helpers ====================

    /**
     * 抢占流水落账权 — 落账失败即判定该变更此前已生效，跳过库存变更与事件发布并计数。
     */
    private boolean claimStockChange(StockChange change) {
        if (stockLedgerRepository.recordIfAbsent(change)) {
            return true;
        }
        log.info(
                "库存变更已落账，跳过重复执行: type={} bizId={} productId={} delta={}",
                change.changeType(),
                change.bizId(),
                change.productId().value(),
                change.delta());
        businessMetricsService.incrementStockChangeSkipped();
        return false;
    }

    /**
     * 卖家 / 管理端直接改库存时补落一条人工调整流水，否则对账任务会把这次人工变更判成漂移。
     */
    private void recordManualAdjustment(Product before, Product after) {
        int delta = after.getStock().value() - before.getStock().value();
        if (delta == 0) {
            return;
        }
        stockLedgerRepository.record(new StockChange(
                StockChangeType.ADJUST,
                null,
                after.getId(),
                delta,
                after.getStock().value()));
    }

    private static <T, R> R mapIfPresent(T value, Function<T, R> mapper) {
        return value != null ? mapper.apply(value) : null;
    }

    private ConditionLevel parseConditionLevel(String code) {
        if (code == null) return null;
        try {
            return ConditionLevel.fromCode(code);
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of(ProductResultCode.INVALID_CONDITION_LEVEL, "无效的成色等级: " + code);
        }
    }

    private void mutate(Product product, Function<Product, Transition<Product, ?>> fn) {
        var result = fn.apply(product);
        productRepository.save(result.aggregate());
        domainEventPublisher.publish(result.event());
    }

    private void mutateIfPresent(Product product, Function<Product, Optional<? extends Transition<Product, ?>>> fn) {
        fn.apply(product).ifPresent(t -> {
            productRepository.save(t.aggregate());
            domainEventPublisher.publish(t.event());
        });
    }

    private Product findByIdOrThrow(ProductId id) {
        return productRepository.findById(id).orElseThrow(() -> ProductDomainException.notFound(id));
    }
}
