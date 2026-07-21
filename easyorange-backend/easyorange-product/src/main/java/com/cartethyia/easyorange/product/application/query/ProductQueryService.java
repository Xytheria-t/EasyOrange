package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.query.assembler.ProductReadModelAssembler;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository.ProductImageInfo;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;
    private final ProductReadModelAssembler readModelAssembler;
    private final ProductCachePort<ProductVO> productCachePort;

    // ── Public query API (single → multi → computed → paged) ──

    @Transactional(readOnly = true)
    public ProductReadModel getProductReadModel(String id) {
        return productQueryRepository.findProductById(id);
    }

    @Transactional(readOnly = true)
    public ProductVO getProductById(String id) {
        var cachedProduct = productCachePort.getProductCache(id);
        if (cachedProduct.isPresent()) {
            return cachedProduct.get();
        }

        var product = productRepository.findById(ProductId.of(id))
                .orElseThrow(() -> new ProductNotFoundException(ProductId.of(id)));
        var productVO = assembleProductVO(product);

        productCachePort.setProductCache(id, productVO);
        return productVO;
    }

    @Transactional(readOnly = true)
    public List<ProductVO> getProductsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        var productIds = ids.stream().map(ProductId::of).toList();
        var products = productRepository.findByIds(productIds);
        return assembleProductVOs(products);
    }

    @Transactional(readOnly = true)
    public List<ProductVO> getSimilarProducts(String productId, Integer limit) {
        var product = productQueryRepository.findProductById(productId);
        if (product == null || product.categoryId() == null) {
            return List.of();
        }

        int effectiveLimit = limit != null ? limit : 10;
        var page = productQueryRepository.searchProducts(
                null, product.categoryId(), null, null, null, null, null, null, 1, effectiveLimit + 1);

        var similarRecords = page.records().stream()
                .filter(p -> !p.id().equals(productId))
                .limit(effectiveLimit)
                .toList();

        return enrichRecords(similarRecords);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductVO> listProducts(String keyword, String categoryId, Integer status,
                                              BigDecimal minPrice, BigDecimal maxPrice,
                                              Integer conditionLevel, String sort,
                                              Boolean hasDiscount,
                                              Integer pageNum, Integer pageSize) {
        int effectivePageNum = pageNum != null ? pageNum : 1;
        int effectivePageSize = pageSize != null ? pageSize : 20;

        var page = productQueryRepository.searchProducts(
                keyword, categoryId, status, minPrice, maxPrice,
                conditionLevel, sort, hasDiscount,
                effectivePageNum, effectivePageSize);

        var vos = enrichPage(page);
        return PageResult.of(vos, page.total(), effectivePageNum, effectivePageSize);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductVO> getMyProducts(String sellerId, Integer status, Integer pageNum, Integer pageSize) {
        var page = productQueryRepository.findProductsBySellerId(sellerId, status, pageNum, pageSize);
        var vos = enrichPage(page);
        return PageResult.of(vos, page.total(), pageNum, pageSize);
    }

    // ── Shared data fetch (leaf helpers) ──

    private Map<String, List<ProductImageInfo>> fetchImages(List<String> productIds) {
        if (productIds.isEmpty()) return Map.of();
        return productQueryRepository.findImagesByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(ProductImageInfo::productId));
    }

    private Map<String, SellerReadModel> fetchSellers(Set<String> sellerIds) {
        if (sellerIds.isEmpty()) return Map.of();
        return productQueryRepository.findSellersByIds(sellerIds).stream()
                .collect(Collectors.toMap(SellerReadModel::id, s -> s, (a, _) -> a));
    }

    // ── ReadModel assembly path (for listing / similar queries) ──

    private ProductVO assembleFromReadModel(ProductReadModel m,
                                             Map<String, List<ProductImageInfo>> imagesByProduct,
                                             Map<String, SellerReadModel> sellerMap) {
        var seller = sellerMap.get(m.sellerId());
        var username = seller != null
                ? (seller.nickName() != null ? seller.nickName() : seller.username())
                : m.username();
        var userAvatar = seller != null ? seller.avatar() : m.userAvatar();

        var images = imagesByProduct.getOrDefault(m.id(), List.of());
        var imageUrls = images.stream().map(ProductImageInfo::imageUrl).toList();
        var mainImageUrl = images.stream()
                .filter(ProductImageInfo::isMain)
                .findFirst()
                .map(ProductImageInfo::imageUrl)
                .orElseGet(() -> imageUrls.isEmpty() ? "" : imageUrls.getFirst());

        return ProductVO.builder()
                .id(m.id()).sellerId(m.sellerId())
                .username(username).userAvatar(userAvatar)
                .categoryId(m.categoryId()).categoryName(m.categoryName())
                .title(m.title()).description(m.description())
                .price(m.price()).originalPrice(m.originalPrice())
                .stock(m.stock()).status(m.status()).statusDesc(m.statusDesc())
                .views(m.views()).condition(m.condition()).conditionDesc(m.conditionDesc())
                .location(m.location()).contactMethod(m.contactMethod())
                .images(imageUrls).mainImageUrl(mainImageUrl)
                .createTime(m.createTime()).updateTime(m.updateTime())
                .build();
    }

    private List<ProductVO> enrichRecords(List<ProductReadModel> records) {
        var productIds = records.stream().map(ProductReadModel::id).toList();
        var imagesByProduct = fetchImages(productIds);
        var sellerIds = records.stream()
                .map(ProductReadModel::sellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        var sellerMap = fetchSellers(sellerIds);
        return records.stream()
                .map(m -> assembleFromReadModel(m, imagesByProduct, sellerMap))
                .toList();
    }

    private List<ProductVO> enrichPage(PageResult<ProductReadModel> page) {
        return enrichRecords(page.records());
    }

    // ── Aggregate assembly path (for getById / getByIds) ──

    private ProductVO assembleProductVO(Product product) {
        if (product == null) return null;
        var productId = product.getId().value();
        var categoryIds = product.getCategoryId() != null
                ? List.of(product.getCategoryId().value()) : List.<String>of();
        var sellerIds = product.getSellerId() != null
                ? Set.of(product.getSellerId().value()) : Set.<String>of();

        var imagesByProduct = productQueryRepository
                .findImagesByProductIds(List.of(productId)).stream()
                .collect(Collectors.groupingBy(ProductImageInfo::productId));
        var categoryMap = productQueryRepository
                .findCategoriesByIds(categoryIds).stream()
                        .collect(Collectors.toMap(ProductQueryRepository.CategoryInfo::id, c -> c, (a, _) -> a));
        var detailMap = productQueryRepository
                .findDetailsByProductIds(List.of(productId)).stream()
                .collect(Collectors.toMap(ProductQueryRepository.ProductDetailInfo::productId, d -> d, (a, _) -> a));
        var sellerMap = fetchSellers(sellerIds);

        return readModelAssembler.toProductVO(product, imagesByProduct, categoryMap, detailMap, sellerMap);
    }

    private List<ProductVO> assembleProductVOs(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }

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
                ? Map.<String, ProductQueryRepository.CategoryInfo>of()
                : productQueryRepository.findCategoriesByIds(categoryIds).stream()
                        .collect(Collectors.toMap(ProductQueryRepository.CategoryInfo::id, c -> c, (a, _) -> a));
        var detailMap = productQueryRepository
                .findDetailsByProductIds(productIds).stream()
                .collect(Collectors.toMap(ProductQueryRepository.ProductDetailInfo::productId, d -> d, (a, _) -> a));
        var sellerMap = fetchSellers(sellerIds);

        return products.stream()
                .map(product -> readModelAssembler.toProductVO(product, imagesByProduct, categoryMap, detailMap, sellerMap))
                .toList();
    }
}
