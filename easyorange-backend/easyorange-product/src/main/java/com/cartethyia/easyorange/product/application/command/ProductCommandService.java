package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.application.command.dto.CreateProductCommand;
import com.cartethyia.easyorange.product.application.command.dto.DecrementStockCommand;
import com.cartethyia.easyorange.product.application.command.dto.DeleteProductCommand;
import com.cartethyia.easyorange.product.application.command.dto.MarkAsSoldCommand;
import com.cartethyia.easyorange.product.application.command.dto.RestoreStockCommand;
import com.cartethyia.easyorange.product.application.command.dto.UpdateProductCommand;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.exception.InvalidProductStatusException;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductCreatedResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductUpdatedResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductDeletedResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductMarkedSoldResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductSubmittedForReviewResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.StockDecreasedResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.StockRestoredResult;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.valueobject.ProductDescription;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.entity.ProductAuditLog;
import com.cartethyia.easyorange.product.domain.repository.ProductAuditLogRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductTitle;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import com.cartethyia.easyorange.product.domain.valueobject.TradeLocation;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ProductCommandService {

    private final ProductRepository productRepository;
    private final ProductCachePort<?> productCachePort;
    private final DomainEventPublisher domainEventPublisher;
    private final ProductAuditLogRepository productAuditLogRepository;

    public Long createProduct(CreateProductCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        ProductCreatedResult result = Product.create(
                SellerId.of(userId),
                CategoryId.of(command.getCategoryId()),
                ProductTitle.of(command.getName()),
                Money.of(command.getPrice()),
                command.getOriginalPrice() != null ? Money.of(command.getOriginalPrice()) : null,
                StockQuantity.of(command.getStock() != null ? command.getStock() : 1),
                ConditionLevel.fromCode(command.getConditionLevel()),
                TradeLocation.of(command.getLocation()),
                ContactMethod.of(command.getContactMethod()),
                ProductDescription.of(command.getDescription()),
                ImageSet.of(command.getImageUrls())
        );
        Product saved = productRepository.save(result.product());
        domainEventPublisher.publish(result.event());

        return saved.getId().value();
    }

    public void updateProduct(UpdateProductCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        ProductId productId = ProductId.of(command.getId());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        if (!product.getSellerId().equals(SellerId.of(userId))) {
            throw new IllegalStateException("无权操作此商品");
        }

        ProductUpdatedResult result = product.update(
                command.getCategoryId() != null ? CategoryId.of(command.getCategoryId()) : null,
                command.getName() != null ? ProductTitle.of(command.getName()) : null,
                command.getPrice() != null ? Money.of(command.getPrice()) : null,
                command.getOriginalPrice() != null ? Money.of(command.getOriginalPrice()) : null,
                command.getStock() != null ? StockQuantity.of(command.getStock()) : null,
                command.getConditionLevel() != null ? ConditionLevel.fromCode(command.getConditionLevel()) : null,
                command.getLocation() != null ? TradeLocation.of(command.getLocation()) : null,
                command.getContactMethod() != null ? ContactMethod.of(command.getContactMethod()) : null,
                command.getDescription() != null ? ProductDescription.of(command.getDescription()) : null,
                command.getImageUrls() != null ? ImageSet.of(command.getImageUrls()) : null
        );
        productRepository.update(result.product());
        domainEventPublisher.publish(result.event());
        productCachePort.evictProductCache(result.product().getId().value());
    }

    public void deleteProduct(DeleteProductCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        ProductId productId = ProductId.of(command.getId());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        ProductDeletedResult result = product.delete(userId);
        productRepository.delete(productId);
        domainEventPublisher.publish(result.event());
        productCachePort.evictProductCache(productId.value());
    }

    public void decrementStock(DecrementStockCommand command) {
        ProductId productId = ProductId.of(command.getProductId());
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        StockDecreasedResult result = product.decrementStock(command.getQuantity() != null ? command.getQuantity() : 1);
        productRepository.update(result.product());
        domainEventPublisher.publish(result.event());
        productCachePort.evictProductCache(result.product().getId().value());
    }

    public void restoreStock(RestoreStockCommand command) {
        ProductId productId = ProductId.of(command.getProductId());
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        StockRestoredResult result = product.restoreStock();
        productRepository.update(result.product());
        domainEventPublisher.publish(result.event());
        productCachePort.evictProductCache(result.product().getId().value());
    }

    public void putOnline(Long productId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        Product product = productRepository.findById(ProductId.of(productId))
                .orElseThrow(() -> new ProductNotFoundException(ProductId.of(productId)));
        if (!product.getSellerId().equals(SellerId.of(userId))) {
            throw new IllegalStateException("无权操作此商品");
        }
        product = product.putOnline();
        productRepository.update(product);
        productCachePort.evictProductCache(productId);
    }

    public void takeOffline(Long productId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        Product product = productRepository.findById(ProductId.of(productId))
                .orElseThrow(() -> new ProductNotFoundException(ProductId.of(productId)));
        if (!product.getSellerId().equals(SellerId.of(userId))) {
            throw new IllegalStateException("无权操作此商品");
        }
        product = product.takeOffline();
        productRepository.update(product);
        productCachePort.evictProductCache(productId);
    }

    public void markAsSold(MarkAsSoldCommand command) {
        ProductId productId = ProductId.of(command.getProductId());
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        ProductMarkedSoldResult result = product.markAsSold();
        productRepository.update(result.product());
        domainEventPublisher.publish(result.event());
        productCachePort.evictProductCache(result.product().getId().value());
    }

    public void submitForReview(Long productId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        ProductId pid = ProductId.of(productId);

        Product product = productRepository.findById(pid)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        int beforeStatus = product.getStatus().getCode();
        ProductSubmittedForReviewResult result = product.submitForReview(userId);
        productRepository.update(result.product());
        domainEventPublisher.publish(result.event());
        productCachePort.evictProductCache(productId);

        ProductAuditLog auditLog = ProductAuditLog.builder()
                .productId(productId)
                .operatorId(userId)
                .operatorName(SecurityContextUtil.getUserContextOrThrow().username())
                .action(AuditAction.RESUBMIT.getCode())
                .beforeStatus(beforeStatus)
                .afterStatus(ProductStatus.PENDING_REVIEW.getCode())
                .build();
        productAuditLogRepository.save(auditLog);
    }
}
