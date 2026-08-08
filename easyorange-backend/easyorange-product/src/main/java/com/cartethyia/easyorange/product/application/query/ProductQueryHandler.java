package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.port.cache.ProductCachePort;
import com.cartethyia.easyorange.product.application.port.cache.SellerCachePort;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository.CategoryInfo;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository.ProductDetailInfo;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository.ProductImageInfo;
import com.cartethyia.easyorange.product.application.query.assembler.ProductReadModelAssembler;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryHandler {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;
    private final ProductReadModelAssembler readModelAssembler;
    private final ProductCachePort productCachePort;
    private final SellerCachePort sellerCachePort;

    @Transactional(readOnly = true)
    public ProductReadModel getProductReadModel(String id) {
        return productQueryRepository.findProductById(id);
    }

    @Transactional(readOnly = true)
    public ProductVO getProductById(String id) {
        return productCachePort
                .getProductCache(id, () -> {
                    var product = productRepository
                            .findById(ProductId.of(id))
                            .orElseThrow(() -> new ProductNotFoundException(ProductId.of(id)));
                    return assembleProductVO(product);
                })
                .orElseThrow(() -> new ProductNotFoundException(ProductId.of(id)));
    }

    @Transactional(readOnly = true)
    public List<ProductVO> getProductsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        var products =
                productRepository.findByIds(ids.stream().map(ProductId::of).toList());
        return assembleProductVOs(products);
    }

    @Transactional(readOnly = true)
    public List<ProductVO> getSimilarProducts(String productId, Integer limit) {
        if (productId == null) return List.of();
        var product = productQueryRepository.findProductById(productId);
        if (product == null || product.categoryId() == null) return List.of();

        int effectiveLimit = Objects.requireNonNullElse(limit, 10);
        var criteria = ProductSearchCriteria.byCategory(product.categoryId(), 1, effectiveLimit + 1);
        var page = productQueryRepository.searchProducts(criteria);

        return enrichRecords(page.records().stream()
                .filter(p -> !p.id().equals(productId))
                .limit(effectiveLimit)
                .toList());
    }

    @Transactional(readOnly = true)
    public PageResult<ProductVO> listProducts(ProductSearchCriteria criteria) {
        var page = productQueryRepository.searchProducts(criteria);
        return PageResult.of(enrichPage(page), page.total(), criteria.effectivePageNum(), criteria.effectivePageSize());
    }

    @Transactional(readOnly = true)
    public PageResult<ProductVO> getMyProducts(String sellerId, String status, Integer pageNum, Integer pageSize) {
        var page = productQueryRepository.findProductsBySellerId(sellerId, status, pageNum, pageSize);
        return PageResult.of(enrichPage(page), page.total(), pageNum, pageSize);
    }

    // ── Shared data fetch (leaf helpers) ──

    private Map<String, List<ProductImageInfo>> fetchImages(List<String> productIds) {
        if (productIds.isEmpty()) return Map.of();
        return productQueryRepository.findImagesByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(ProductImageInfo::productId));
    }

    // ── ReadModel assembly path (for listing / similar queries) ──

    private List<ProductVO> enrichRecords(List<ProductReadModel> records) {
        var productIds = records.stream().map(ProductReadModel::id).toList();
        var imagesByProduct = fetchImages(productIds);
        var sellerIds = records.stream()
                .map(ProductReadModel::sellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        var sellerMap = sellerCachePort.getSellers(sellerIds);
        return records.stream()
                .map(m -> readModelAssembler.toProductVO(m, imagesByProduct, sellerMap))
                .toList();
    }

    private List<ProductVO> enrichPage(PageResult<ProductReadModel> page) {
        return enrichRecords(page.records());
    }

    // ── Aggregate assembly path ──

    private ProductVO assembleProductVO(Product product) {
        var ctx = fetchAssemblyContext(List.of(product));
        return readModelAssembler.toProductVO(product, ctx);
    }

    private List<ProductVO> assembleProductVOs(List<Product> products) {
        if (products == null || products.isEmpty()) return List.of();
        var ctx = fetchAssemblyContext(products);
        return products.stream()
                .map(p -> readModelAssembler.toProductVO(p, ctx))
                .toList();
    }

    private ProductReadModelAssembler.AssemblyContext fetchAssemblyContext(List<Product> products) {
        var productIds = products.stream().map(p -> p.getId().value()).toList();
        var categoryIds = products.stream()
                .filter(p -> p.getCategoryId() != null)
                .map(p -> p.getCategoryId().value())
                .distinct()
                .toList();
        var sellerIds = products.stream()
                .filter(p -> p.getSellerId() != null)
                .map(p -> p.getSellerId().value())
                .collect(Collectors.toSet());
        var imagesByProduct = fetchImages(productIds);
        var categoryMap = categoryIds.isEmpty()
                ? Map.<String, CategoryInfo>of()
                : productQueryRepository.findCategoriesByIds(categoryIds).stream()
                        .collect(Collectors.toMap(CategoryInfo::id, c -> c, (a, _) -> a));
        var detailMap = productQueryRepository.findDetailsByProductIds(productIds).stream()
                .collect(Collectors.toMap(ProductDetailInfo::productId, d -> d, (a, _) -> a));
        var sellerMap = sellerCachePort.getSellers(sellerIds);
        return new ProductReadModelAssembler.AssemblyContext(imagesByProduct, categoryMap, detailMap, sellerMap);
    }
}
