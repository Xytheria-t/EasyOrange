package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.ProductCreateSpec;
import com.cartethyia.easyorange.product.domain.aggregate.ProductUpdateSpec;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.exception.ProductNotOwnerException;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ProductCommandService {

    private final ProductRepository productRepository;
    private final DomainEventPublisher domainEventPublisher;

    // ==================== CRUD ====================

    public String createProduct(CreateProductCommand command) {
        var userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        var product = Product.create(
                new ProductCreateSpec(
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
                        ImageSet.of(command.imageUrls())
                )
        );

        var created = productRepository.create(product.aggregate());
        domainEventPublisher.publish(product.event());
        return created.getId().value();
    }

    public void updateProduct(UpdateProductCommand command) {
        var productId = ProductId.of(command.id());
        var product = verifyOwnership(productId, SecurityContextUtil.getCurrentUserIdOrThrow());

        mutate(product, p -> p.update(
                new ProductUpdateSpec(
                        mapIfPresent(command.categoryId(), CategoryId::of),
                        mapIfPresent(command.name(), ProductTitle::of),
                        mapIfPresent(command.price(), Money::of),
                        mapIfPresent(command.originalPrice(), Money::of),
                        mapIfPresent(command.stock(), StockQuantity::of),
                        command.conditionLevel() != null ? parseConditionLevel(command.conditionLevel()) : null,
                        mapIfPresent(command.location(), TradeLocation::of),
                        mapIfPresent(command.contactMethod(), ContactMethod::of),
                        mapIfPresent(command.description(), ProductDescription::of),
                        mapIfPresent(command.imageUrls(), ImageSet::of)
                )
        ));
    }

    public void deleteProduct(String id) {
        var userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        var pid = ProductId.of(id);
        var product = findByIdOrThrow(pid);

        var t = product.delete(userId);
        productRepository.delete(pid);
        domainEventPublisher.publish(t.event());
    }

    // ==================== Stock ====================

    public void decrementStock(String productId, Integer quantity) {
        var product = findByIdOrThrow(ProductId.of(productId));
        mutate(product, p -> p.decrementStock(quantity != null ? quantity : 1));
    }

    public void restoreStock(String productId) {
        restoreStock(productId, null);
    }

    public void restoreStock(String productId, Integer quantity) {
        var product = findByIdOrThrow(ProductId.of(productId));
        mutate(product, p -> p.restoreStock(quantity != null ? quantity : 1));
    }

    // ==================== Status Transitions ====================

    public void submitForReview(String productId) {
        var userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        var product = findByIdOrThrow(ProductId.of(productId));
        mutate(product, p -> p.submitForReview(userId));
    }

    public void putOnline(String productId) {
        var product = findByIdOrThrow(ProductId.of(productId));
        mutate(product, Product::putOnline);
    }

    public void takeOffline(String productId) {
        var userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        var product = verifyOwnership(ProductId.of(productId), userId);
        mutate(product, Product::takeOffline);
    }

    public void markAsSold(String productId) {
        var product = findByIdOrThrow(ProductId.of(productId));
        if (product.getStatus() == ProductStatus.SOLD) {
            log.info("Product {} already SOLD, skipping idempotent call", productId);
            return;
        }
        mutate(product, Product::markAsSold);
    }

    // ==================== Private Helpers ====================

    private static <T, R> R mapIfPresent(T value, Function<T, R> mapper) {
        return value != null ? mapper.apply(value) : null;
    }

    private ConditionLevel parseConditionLevel(String code) {
        if (code == null) return null;
        try {
            return ConditionLevel.fromCode(code);
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of("无效的成色等级: " + code);
        }
    }

    private void mutate(Product product, Function<Product, Transition<Product, ?>> fn) {
        var result = fn.apply(product);
        productRepository.update(result.aggregate());
        domainEventPublisher.publish(result.event());
    }

    private Product findByIdOrThrow(ProductId id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Product verifyOwnership(ProductId productId, String userId) {
        var product = findByIdOrThrow(productId);
        if (!product.getSellerId().equals(SellerId.of(userId))) {
            throw new ProductNotOwnerException(productId);
        }
        return product;
    }
}
