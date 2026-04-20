package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.assembler.ProductAssembler;
import com.cartethyia.easyorange.product.domain.aggregate.ProductAggregate;
import com.cartethyia.easyorange.product.domain.event.StockDecreasedEvent;
import com.cartethyia.easyorange.product.domain.event.StockRestoredEvent;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import com.cartethyia.easyorange.product.entity.Category;
import com.cartethyia.easyorange.product.entity.Product;
import com.cartethyia.easyorange.product.entity.ProductDetail;
import com.cartethyia.easyorange.product.entity.ProductImage;
import com.cartethyia.easyorange.product.mapper.ProductDetailMapper;
import com.cartethyia.easyorange.product.mapper.ProductMapper;
import com.cartethyia.easyorange.product.service.CategoryService;
import com.cartethyia.easyorange.product.service.ProductImageService;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCommandHandler {

    private final ProductRepository productRepository;
    private final ProductImageService productImageService;
    private final CategoryService categoryService;
    private final ProductDetailMapper productDetailMapper;
    private final ProductMapper productMapper;
    private final ProductAssembler productAssembler;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public ProductVO handle(CreateProductCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();

        ProductAggregate aggregate = ProductAggregate.create(command, userId);

        productRepository.save(aggregate.getProduct());

        if (aggregate.hasDetail()) {
            aggregate.getDetail().setProductId(aggregate.getProduct().getId());
            productDetailMapper.insert(aggregate.getDetail());
        }

        if (aggregate.hasImages()) {
            for (ProductImage image : aggregate.getImages()) {
                image.setProductId(aggregate.getProduct().getId());
            }
            productImageService.saveBatch(aggregate.getImages());
        }

        for (BaseDomainEvent event : aggregate.releaseEvents()) {
            domainEventPublisher.publish(event);
        }

        log.info("创建商品成功: productId={}, userId={}", aggregate.getProduct().getId(), userId);

        return buildProductVO(aggregate.getProduct());
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductVO handle(UpdateProductCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        Product product = validateAndGetOwnedProduct(command.getId());

        ProductAggregate aggregate = loadAggregate(product);
        aggregate.update(command);

        productRepository.update(aggregate.getProduct());

        if (aggregate.hasDetail()) {
            ProductDetail detail = productDetailMapper.selectById(command.getId());
            if (detail != null) {
                detail.setDescription(aggregate.getDetail().getDescription());
                productDetailMapper.updateById(detail);
            } else {
                aggregate.getDetail().setProductId(product.getId());
                productDetailMapper.insert(aggregate.getDetail());
            }
        }

        if (command.getImageUrls() != null) {
            productImageService.deleteByProductId(command.getId());
            if (!command.getImageUrls().isEmpty()) {
                for (ProductImage image : aggregate.getImages()) {
                    image.setProductId(command.getId());
                }
                productImageService.saveBatch(aggregate.getImages());
            }
        }

        for (BaseDomainEvent event : aggregate.releaseEvents()) {
            domainEventPublisher.publish(event);
        }

        log.info("更新商品成功: productId={}, userId={}", command.getId(), userId);

        return buildProductVO(aggregate.getProduct());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(DeleteProductCommand command) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        Product product = validateAndGetOwnedProduct(command.getId());

        ProductAggregate aggregate = loadAggregate(product);
        aggregate.delete(userId);

        productRepository.removeById(command.getId());
        productDetailMapper.deleteById(command.getId());
        productImageService.deleteByProductId(command.getId());

        for (BaseDomainEvent event : aggregate.releaseEvents()) {
            domainEventPublisher.publish(event);
        }

        log.info("删除商品成功: productId={}, userId={}", command.getId(), userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(DecrementStockCommand command) {
        Product product = productRepository.findById(command.getProductId());
        BizRequire.notNull(product, "商品不存在");

        ProductAggregate aggregate = ProductAggregate.load(product, null, List.of());
        StockDecreasedEvent event = aggregate.decrementStock();

        productRepository.update(aggregate.getProduct());

        domainEventPublisher.publish(event);

        log.info("扣减库存成功: productId={}", command.getProductId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(RestoreStockCommand command) {
        Product product = productRepository.findById(command.getProductId());
        if (product == null) {
            log.warn("库存恢复失败: productId={}", command.getProductId());
            return;
        }

        ProductAggregate aggregate = ProductAggregate.load(product, null, List.of());
        StockRestoredEvent event = aggregate.restoreStock();

        productRepository.update(aggregate.getProduct());

        domainEventPublisher.publish(event);

        log.info("恢复库存成功: productId={}", command.getProductId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(MarkAsSoldCommand command) {
        Product product = productRepository.findById(command.getProductId());
        if (product == null) {
            log.warn("标记售出失败: productId={}", command.getProductId());
            return;
        }

        ProductAggregate aggregate = ProductAggregate.load(product, null, List.of());
        aggregate.markAsSold();

        productRepository.update(aggregate.getProduct());

        for (BaseDomainEvent event : aggregate.releaseEvents()) {
            domainEventPublisher.publish(event);
        }

        log.info("标记售出成功: productId={}", command.getProductId());
    }

    private Product validateAndGetOwnedProduct(Long id) {
        Product product = productRepository.findById(id);
        BizRequire.notNull(product, "商品不存在");
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        BizRequire.isTrue(product.getUserId().equals(userId), "无权操作此商品");
        return product;
    }

    private ProductAggregate loadAggregate(Product product) {
        ProductDetail detail = productDetailMapper.selectById(product.getId());
        List<ProductImage> images = productImageService.listByProductIds(List.of(product.getId()));
        return ProductAggregate.load(product, detail, images);
    }

    private ProductVO buildProductVO(Product product) {
        if (product == null) {
            return null;
        }
        List<Long> productIds = List.of(product.getId());
        List<Long> categoryIds = product.getCategoryId() != null
                ? List.of(product.getCategoryId()) : List.of();
        Set<Long> sellerIds = product.getUserId() != null
                ? Set.of(product.getUserId()) : Set.of();

        return buildProductVO(product, productIds, categoryIds, sellerIds);
    }

    private ProductVO buildProductVO(Product product, List<Long> productIds,
                                    List<Long> categoryIds, Set<Long> sellerIds) {
        Map<Long, List<ProductImage>> imagesByProduct = productAssembler.groupImagesByProduct(
                productImageService.listByProductIds(productIds));
        Map<Long, Category> categoryMap = productAssembler.buildCategoryMap(
                categoryIds.isEmpty() ? List.of() : categoryService.listByIds(categoryIds));
        Map<Long, ProductDetail> detailMap = productAssembler.buildDetailMap(
                listDetailsByProductIds(productIds));
        Map<Long, com.cartethyia.easyorange.product.dto.vo.SellerInfo> sellerMap = productAssembler.buildSellerMap(
                sellerIds.isEmpty() ? List.of() : listSellersByIds(sellerIds));

        return productAssembler.toProductVO(product, imagesByProduct, categoryMap, detailMap, sellerMap);
    }

    private List<ProductDetail> listDetailsByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return productMapper.selectDetailsByProductIds(productIds);
    }

    private List<com.cartethyia.easyorange.product.dto.vo.SellerInfo> listSellersByIds(Set<Long> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty()) {
            return List.of();
        }
        return productMapper.selectSellersByIds(sellerIds);
    }
}