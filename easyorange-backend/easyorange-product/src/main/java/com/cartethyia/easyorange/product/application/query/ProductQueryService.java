package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.Singleflight;
import com.cartethyia.easyorange.product.application.query.assembler.ProductReadModelAssembler;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.application.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository.ProductImageInfo;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;
    private final ProductReadModelAssembler readModelAssembler;
    private final ProductCachePort productCachePort;
    private final Singleflight<String, ProductVO> productSingleflight = new Singleflight<>();

    /**
     * 卖家信息内存缓存 — 卖家信息极少变更，避免重复查询相同卖家。
     * TODO: 未来可将商品 + 卖家信息合并为一次带 JOIN 的查询，减少 round trip
     */
    private final Map<String, SellerReadModel> sellerCache = new ConcurrentHashMap<>();

    // ── Public query API (single → multi → computed → paged) ──

    @Transactional(readOnly = true)
    public ProductReadModel getProductReadModel(String id) {
        return productQueryRepository.findProductById(id);
    }

    @Transactional(readOnly = true)
    public ProductVO getProductById(String id) {
        var cachedProduct = productCachePort.getProductCache(id);
        return cachedProduct.orElseGet(() -> productSingleflight.execute(id, () -> {
            // double-check cache（其他线程可能已回填）
            var cached = productCachePort.getProductCache(id);
            if (cached.isPresent()) {
                return cached.get();
            }
            var product = productRepository.findById(ProductId.of(id))
                    .orElseThrow(() -> new ProductNotFoundException(ProductId.of(id)));
            var productVO = assembleProductVOs(List.of(product)).getFirst();
            productCachePort.setProductCache(id, productVO);
            return productVO;
        }));

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
        var criteria = new ProductSearchCriteria(
                null, product.categoryId(), null, null, null, null, null, null, 1, effectiveLimit + 1);
        var page = productQueryRepository.searchProducts(criteria);

        var similarRecords = page.records().stream()
                .filter(p -> !p.id().equals(productId))
                .limit(effectiveLimit)
                .toList();

        return enrichRecords(similarRecords);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductVO> listProducts(ProductSearchCriteria criteria) {
        var page = productQueryRepository.searchProducts(criteria);
        var vos = enrichPage(page);
        return PageResult.of(vos, page.total(), criteria.effectivePageNum(), criteria.effectivePageSize());
    }

    @Transactional(readOnly = true)
    public PageResult<ProductVO> getMyProducts(String sellerId, String status, Integer pageNum, Integer pageSize) {
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

        var result = new HashMap<String, SellerReadModel>();
        var missingIds = new HashSet<String>();
        for (var id : sellerIds) {
            var cached = sellerCache.get(id);
            if (cached != null) {
                result.put(id, cached);
            } else {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            var fetched = productQueryRepository.findSellersByIds(missingIds).stream()
                    .collect(Collectors.toMap(SellerReadModel::id, s -> s, (a, _) -> a));
            sellerCache.putAll(fetched);
            result.putAll(fetched);
        }

        return result;
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
