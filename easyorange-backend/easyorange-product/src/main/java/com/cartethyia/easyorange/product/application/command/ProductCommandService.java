package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.application.command.dto.CreateProductCommand;
import com.cartethyia.easyorange.product.application.command.dto.DecrementStockCommand;
import com.cartethyia.easyorange.product.application.command.dto.DeleteProductCommand;
import com.cartethyia.easyorange.product.application.command.dto.MarkAsSoldCommand;
import com.cartethyia.easyorange.product.application.command.dto.RestoreStockCommand;
import com.cartethyia.easyorange.product.application.command.dto.UpdateProductCommand;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.repository.ProductCachePort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.ConditionLevelVO;
import com.cartethyia.easyorange.product.domain.valueobject.ContactMethod;
import com.cartethyia.easyorange.product.domain.valueobject.ImageSet;
import com.cartethyia.easyorange.product.domain.valueobject.Money;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ProductCommandService {

    private final ProductRepository productRepository;
    private final ProductCachePort productCachePort;
    private final DomainEventPublisher domainEventPublisher;

    public Long createProduct(CreateProductCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        Product product = Product.create(
                SellerId.of(userId),
                CategoryId.of(command.getCategoryId()),
                ProductTitle.of(command.getName()),
                Money.of(command.getPrice()),
                command.getOriginalPrice() != null ? Money.of(command.getOriginalPrice()) : null,
                StockQuantity.of(command.getStock() != null ? command.getStock() : 1),
                ConditionLevelVO.of(command.getConditionLevel()),
                TradeLocation.of(command.getLocation()),
                ContactMethod.of(command.getContactMethod()),
                ProductDescription.of(command.getDescription()),
                ImageSet.of(command.getImageUrls())
        );
        productRepository.save(product);
        publishEvents(product);

        log.info("创建商品成功：productId={}, userId={}", product.getId().value(), userId);
        return product.getId().value();
    }

    public Long updateProduct(UpdateProductCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        ProductId productId = ProductId.of(command.getId());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        if (!product.getSellerId().equals(SellerId.of(userId))) {
            throw new IllegalStateException("无权操作此商品");
        }

        product.update(
                command.getCategoryId() != null ? CategoryId.of(command.getCategoryId()) : null,
                command.getName() != null ? ProductTitle.of(command.getName()) : null,
                command.getPrice() != null ? Money.of(command.getPrice()) : null,
                command.getOriginalPrice() != null ? Money.of(command.getOriginalPrice()) : null,
                command.getStock() != null ? StockQuantity.of(command.getStock()) : null,
                command.getConditionLevel() != null ? ConditionLevelVO.of(command.getConditionLevel()) : null,
                command.getLocation() != null ? TradeLocation.of(command.getLocation()) : null,
                command.getContactMethod() != null ? ContactMethod.of(command.getContactMethod()) : null,
                command.getDescription() != null ? ProductDescription.of(command.getDescription()) : null,
                command.getImageUrls() != null ? ImageSet.of(command.getImageUrls()) : null
        );
        productRepository.update(product);
        publishEvents(product);
        productCachePort.evictProductCache(product.getId().value());

        log.info("更新商品成功: productId={}, userId={}", command.getId(), userId);
        return product.getId().value();
    }

    public void deleteProduct(DeleteProductCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        ProductId productId = ProductId.of(command.getId());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.delete(userId);
        productRepository.delete(productId);
        publishEvents(product);
        productCachePort.evictProductCache(productId.value());

        log.info("删除商品成功: productId={}, userId={}", command.getId(), userId);
    }

    public void decrementStock(DecrementStockCommand command) {
        ProductId productId = ProductId.of(command.getProductId());
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.decrementStock();
        productRepository.update(product);
        publishEvents(product);
        productCachePort.evictProductCache(productId.value());

        log.info("扣减库存成功: productId={}", command.getProductId());
    }

    public void restoreStock(RestoreStockCommand command) {
        ProductId productId = ProductId.of(command.getProductId());
        Product product = productRepository.findById(productId)
                .orElse(null);
        if (product == null) {
            log.warn("库存恢复失败: productId={}", command.getProductId());
            return;
        }

        product.restoreStock();
        productRepository.update(product);
        publishEvents(product);
        productCachePort.evictProductCache(productId.value());

        log.info("恢复库存成功: productId={}", command.getProductId());
    }

    public void putOnline(Long productId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        Product product = productRepository.findById(ProductId.of(productId))
                .orElseThrow(() -> new ProductNotFoundException(ProductId.of(productId)));
        if (!product.getSellerId().equals(SellerId.of(userId))) {
            throw new IllegalStateException("无权操作此商品");
        }
        product.putOnline();
        productRepository.update(product);
        publishEvents(product);
        productCachePort.evictProductCache(productId);
        log.info("商品上架成功: productId={}, userId={}", productId, userId);
    }

    public void takeOffline(Long productId) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        Product product = productRepository.findById(ProductId.of(productId))
                .orElseThrow(() -> new ProductNotFoundException(ProductId.of(productId)));
        if (!product.getSellerId().equals(SellerId.of(userId))) {
            throw new IllegalStateException("无权操作此商品");
        }
        product.takeOffline();
        productRepository.update(product);
        publishEvents(product);
        productCachePort.evictProductCache(productId);
        log.info("商品下架成功: productId={}, userId={}", productId, userId);
    }

    public void markAsSold(MarkAsSoldCommand command) {
        ProductId productId = ProductId.of(command.getProductId());
        Product product = productRepository.findById(productId)
                .orElse(null);
        if (product == null) {
            log.warn("标记售出失败: productId={}", command.getProductId());
            return;
        }

        product.markAsSold();
        productRepository.update(product);
        publishEvents(product);
        productCachePort.evictProductCache(productId.value());

        log.info("标记售出成功: productId={}", command.getProductId());
    }

    private void publishEvents(Product product) {
        for (BaseDomainEvent event : product.releaseEvents()) {
            domainEventPublisher.publish(event);
        }
    }
}
