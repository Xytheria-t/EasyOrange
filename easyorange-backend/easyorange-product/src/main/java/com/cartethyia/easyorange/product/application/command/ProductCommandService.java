package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductCreatedResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductDeletedResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductMarkedSoldResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductSubmittedForReviewResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.ProductUpdatedResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.StockDecreasedResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product.StockRestoredResult;
import com.cartethyia.easyorange.product.domain.entity.ProductAuditLog;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.repository.ProductAuditLogRepository;
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

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ProductCommandService {

    public record CreateProductCommand(
        String categoryId, String name, BigDecimal price,
        BigDecimal originalPrice, Integer stock, Integer conditionLevel,
        String location, String contactMethod, String description,
        List<String> imageUrls
    ) {}

    public record UpdateProductCommand(
        String id, String categoryId, String name, BigDecimal price,
        BigDecimal originalPrice, Integer stock, Integer conditionLevel,
        String location, String contactMethod, String description,
        List<String> imageUrls
    ) {}

    private final ProductRepository productRepository;
    private final ProductCachePort<?> productCachePort;
    private final DomainEventPublisher domainEventPublisher;
    private final ProductAuditLogRepository productAuditLogRepository;

    public String createProduct(CreateProductCommand command) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        ProductCreatedResult result = Product.create(
                SellerId.of(userId),
                CategoryId.of(command.categoryId()),
                ProductTitle.of(command.name()),
                Money.of(command.price()),
                command.originalPrice() != null ? Money.of(command.originalPrice()) : null,
                StockQuantity.of(command.stock() != null ? command.stock() : 1),
                ConditionLevel.fromCode(command.conditionLevel()),
                TradeLocation.of(command.location()),
                ContactMethod.of(command.contactMethod()),
                ProductDescription.of(command.description()),
                ImageSet.of(command.imageUrls())
        );
        Product saved = productRepository.save(result.product());
        domainEventPublisher.publish(result.event());

        return saved.getId().value();
    }

    public void updateProduct(UpdateProductCommand command) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        ProductId productId = ProductId.of(command.id());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        if (!product.getSellerId().equals(SellerId.of(userId))) {
            throw new IllegalStateException("无权操作此商品");
        }

        ProductUpdatedResult result = product.update(
                command.categoryId() != null ? CategoryId.of(command.categoryId()) : null,
                command.name() != null ? ProductTitle.of(command.name()) : null,
                command.price() != null ? Money.of(command.price()) : null,
                command.originalPrice() != null ? Money.of(command.originalPrice()) : null,
                command.stock() != null ? StockQuantity.of(command.stock()) : null,
                command.conditionLevel() != null ? ConditionLevel.fromCode(command.conditionLevel()) : null,
                command.location() != null ? TradeLocation.of(command.location()) : null,
                command.contactMethod() != null ? ContactMethod.of(command.contactMethod()) : null,
                command.description() != null ? ProductDescription.of(command.description()) : null,
                command.imageUrls() != null ? ImageSet.of(command.imageUrls()) : null
        );
        productRepository.update(result.product());
        domainEventPublisher.publish(result.event());
        productCachePort.evictProductCache(result.product().getId().value());
    }

    public void deleteProduct(String id) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        ProductId productId = ProductId.of(id);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        ProductDeletedResult result = product.delete(userId);
        productRepository.delete(productId);
        domainEventPublisher.publish(result.event());
        productCachePort.evictProductCache(productId.value());
    }

    public void decrementStock(String productId, Integer quantity) {
        ProductId pid = ProductId.of(productId);
        Product product = productRepository.findById(pid)
                .orElseThrow(() -> new ProductNotFoundException(pid));

        StockDecreasedResult result = product.decrementStock(quantity != null ? quantity : 1);
        productRepository.update(result.product());
        domainEventPublisher.publish(result.event());
        productCachePort.evictProductCache(result.product().getId().value());
    }

    public void restoreStock(String productId) {
        ProductId pid = ProductId.of(productId);
        Product product = productRepository.findById(pid)
                .orElseThrow(() -> new ProductNotFoundException(pid));

        StockRestoredResult result = product.restoreStock();
        productRepository.update(result.product());
        domainEventPublisher.publish(result.event());
        productCachePort.evictProductCache(result.product().getId().value());
    }

    public void putOnline(String productId) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        Product product = productRepository.findById(ProductId.of(productId))
                .orElseThrow(() -> new ProductNotFoundException(ProductId.of(productId)));
        if (!product.getSellerId().equals(SellerId.of(userId))) {
            throw new IllegalStateException("无权操作此商品");
        }
        product = product.putOnline();
        productRepository.update(product);
        productCachePort.evictProductCache(productId);
    }

    public void takeOffline(String productId) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        Product product = productRepository.findById(ProductId.of(productId))
                .orElseThrow(() -> new ProductNotFoundException(ProductId.of(productId)));
        if (!product.getSellerId().equals(SellerId.of(userId))) {
            throw new IllegalStateException("无权操作此商品");
        }
        product = product.takeOffline();
        productRepository.update(product);
        productCachePort.evictProductCache(productId);
    }

    public void markAsSold(String productId) {
        ProductId pid = ProductId.of(productId);
        Product product = productRepository.findById(pid)
                .orElseThrow(() -> new ProductNotFoundException(pid));

        ProductMarkedSoldResult result = product.markAsSold();
        productRepository.update(result.product());
        domainEventPublisher.publish(result.event());
        productCachePort.evictProductCache(result.product().getId().value());
    }

    public void submitForReview(String productId) {
        String userId = SecurityContextUtil.getCurrentUserIdOrThrow();
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
