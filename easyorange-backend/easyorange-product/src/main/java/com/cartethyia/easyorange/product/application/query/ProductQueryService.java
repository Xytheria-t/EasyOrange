package com.cartethyia.easyorange.product.application.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import com.cartethyia.easyorange.product.application.query.assembler.ProductReadModelAssembler;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductQueryRequest;
import com.cartethyia.easyorange.product.application.service.ProductViewCountService;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;
    private final ProductReadModelAssembler readModelAssembler;
    private final ProductCachePort productCachePort;
    private final ProductViewCountService viewCountService;

    @Transactional(readOnly = true)
    public PageResult<ProductVO> listProducts(ProductQueryRequest request) {
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        Page<ProductReadModel> page = productQueryRepository.searchProducts(
                request.getKeyword(),
                request.getCategoryId(),
                request.getStatus(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getConditionLevel(),
                request.getSort(),
                pageNum,
                pageSize
        );

        List<Long> productIds = page.getRecords().stream()
                .map(ProductReadModel::id)
                .collect(Collectors.toList());
        Map<Long, List<ProductQueryRepository.ProductImageInfo>> imagesByProduct = productQueryRepository
                .findImagesByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(ProductQueryRepository.ProductImageInfo::productId));

        Set<Long> sellerIds = page.getRecords().stream()
                .map(ProductReadModel::sellerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, SellerReadModel> sellerMap = sellerIds.isEmpty()
                ? Map.of()
                : productQueryRepository.findSellersByIds(sellerIds).stream()
                        .collect(Collectors.toMap(SellerReadModel::id, s -> s, (a, b) -> a));

        List<ProductVO> vos = page.getRecords().stream()
                .map(m -> voFromReadModel(m, imagesByProduct, sellerMap))
                .collect(Collectors.toList());

        return PageResult.of(vos, page.getTotal(), pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductVO> getMyProducts(Long sellerId, Integer status, Integer pageNum, Integer pageSize) {
        Page<ProductReadModel> page = productQueryRepository.findProductsBySellerId(
                sellerId, status, pageNum, pageSize);

        List<Long> productIds = page.getRecords().stream()
                .map(ProductReadModel::id)
                .collect(Collectors.toList());
        Map<Long, List<ProductQueryRepository.ProductImageInfo>> imagesByProduct = productQueryRepository
                .findImagesByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(ProductQueryRepository.ProductImageInfo::productId));

        Set<Long> sellerIds = page.getRecords().stream()
                .map(ProductReadModel::sellerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, SellerReadModel> sellerMap = sellerIds.isEmpty()
                ? Map.of()
                : productQueryRepository.findSellersByIds(sellerIds).stream()
                        .collect(Collectors.toMap(SellerReadModel::id, s -> s, (a, b) -> a));

        List<ProductVO> vos = page.getRecords().stream()
                .map(m -> voFromReadModel(m, imagesByProduct, sellerMap))
                .collect(Collectors.toList());

        return PageResult.of(vos, page.getTotal(), pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public ProductVO getProductById(Long id) {
        ProductVO cachedProduct = productCachePort.getProductCache(id);
        if (cachedProduct != null) {
            return cachedProduct;
        }

        Product product = productRepository.findById(ProductId.of(id))
                .orElseThrow(() -> new ProductNotFoundException(id));
        ProductVO productVO = assembleProductVO(product);

        productCachePort.setProductCache(id, productVO);
        return productVO;
    }

    @Transactional(readOnly = true)
    public List<ProductVO> getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<ProductId> productIds = ids.stream().map(ProductId::of).collect(Collectors.toList());
        List<Product> products = productRepository.findByIds(productIds);
        return assembleProductVOs(products);
    }

    @Transactional(readOnly = true)
    public List<ProductVO> getProductsBySellerId(Long sellerId) {
        List<Product> products = productRepository.findBySellerId(SellerId.of(sellerId));
        return assembleProductVOs(products);
    }

    @Transactional(readOnly = true)
    public ProductReadModel getProductReadModel(Long id) {
        return productQueryRepository.findProductById(id);
    }

    @Transactional(readOnly = true)
    public List<ProductReadModel> getProductReadModels(List<Long> ids) {
        return productQueryRepository.findProductsByIds(ids);
    }

    @Transactional(readOnly = true)
    public List<ProductVO> getSimilarProducts(Long productId, Integer limit) {
        ProductReadModel product = productQueryRepository.findProductById(productId);
        if (product == null || product.categoryId() == null) {
            return List.of();
        }

        int effectiveLimit = limit != null ? limit : 10;
        Page<ProductReadModel> page = productQueryRepository.searchProducts(
                null, product.categoryId(), null, null, null, null, null, 1, effectiveLimit + 1);

        List<Long> similarIds = page.getRecords().stream()
                .filter(p -> !p.id().equals(productId))
                .limit(effectiveLimit)
                .map(ProductReadModel::id)
                .collect(Collectors.toList());
        Map<Long, List<ProductQueryRepository.ProductImageInfo>> imagesByProduct = productQueryRepository
                .findImagesByProductIds(similarIds).stream()
                .collect(Collectors.groupingBy(ProductQueryRepository.ProductImageInfo::productId));

        Set<Long> sellerIds = page.getRecords().stream()
                .filter(p -> !p.id().equals(productId))
                .limit(effectiveLimit)
                .map(ProductReadModel::sellerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, SellerReadModel> sellerMap = sellerIds.isEmpty()
                ? Map.of()
                : productQueryRepository.findSellersByIds(sellerIds).stream()
                        .collect(Collectors.toMap(SellerReadModel::id, s -> s, (a, b) -> a));

        return page.getRecords().stream()
                .filter(p -> !p.id().equals(productId))
                .limit(effectiveLimit)
                .map(m -> voFromReadModel(m, imagesByProduct, sellerMap))
                .collect(Collectors.toList());
    }

    public void incrementViewCount(Long id) {
        viewCountService.incrementViewCount(id);
    }

    private ProductVO assembleProductVO(Product product) {
        if (product == null) {
            return null;
        }
        Long productId = product.getId().value();
        List<Long> productIds = List.of(productId);
        List<Long> categoryIds = product.getCategoryId() != null
                ? List.of(product.getCategoryId().value()) : List.of();
        Set<Long> sellerIds = product.getSellerId() != null
                ? Set.of(product.getSellerId().value()) : Set.of();

        Map<Long, List<ProductQueryRepository.ProductImageInfo>> imagesByProduct = productQueryRepository
                .findImagesByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(ProductQueryRepository.ProductImageInfo::productId));
        Map<Long, ProductQueryRepository.CategoryInfo> categoryMap = productQueryRepository
                .findCategoriesByIds(categoryIds).stream()
                .collect(Collectors.toMap(ProductQueryRepository.CategoryInfo::id, c -> c, (a, b) -> a));
        Map<Long, ProductQueryRepository.ProductDetailInfo> detailMap = productQueryRepository
                .findDetailsByProductIds(productIds).stream()
                .collect(Collectors.toMap(ProductQueryRepository.ProductDetailInfo::productId, d -> d, (a, b) -> a));
        Map<Long, SellerReadModel> sellerMap = productQueryRepository
                .findSellersByIds(sellerIds).stream()
                .collect(Collectors.toMap(SellerReadModel::id, s -> s, (a, b) -> a));

        return readModelAssembler.toProductVO(product, imagesByProduct, categoryMap, detailMap, sellerMap);
    }

    private List<ProductVO> assembleProductVOs(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = products.stream()
                .map(p -> p.getId().value())
                .collect(Collectors.toList());
        List<Long> categoryIds = products.stream()
                .filter(p -> p.getCategoryId() != null)
                .map(p -> p.getCategoryId().value())
                .distinct()
                .collect(Collectors.toList());
        Set<Long> sellerIds = products.stream()
                .filter(p -> p.getSellerId() != null)
                .map(p -> p.getSellerId().value())
                .collect(Collectors.toSet());

        Map<Long, List<ProductQueryRepository.ProductImageInfo>> imagesByProduct = productQueryRepository
                .findImagesByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(ProductQueryRepository.ProductImageInfo::productId));
        Map<Long, ProductQueryRepository.CategoryInfo> categoryMap = categoryIds.isEmpty()
                ? Map.of()
                : productQueryRepository.findCategoriesByIds(categoryIds).stream()
                        .collect(Collectors.toMap(ProductQueryRepository.CategoryInfo::id, c -> c, (a, b) -> a));
        Map<Long, ProductQueryRepository.ProductDetailInfo> detailMap = productQueryRepository
                .findDetailsByProductIds(productIds).stream()
                .collect(Collectors.toMap(ProductQueryRepository.ProductDetailInfo::productId, d -> d, (a, b) -> a));
        Map<Long, SellerReadModel> sellerMap = sellerIds.isEmpty()
                ? Map.of()
                : productQueryRepository.findSellersByIds(sellerIds).stream()
                        .collect(Collectors.toMap(SellerReadModel::id, s -> s, (a, b) -> a));

        return products.stream()
                .map(product -> readModelAssembler.toProductVO(product, imagesByProduct, categoryMap, detailMap, sellerMap))
                .collect(Collectors.toList());
    }

    private ProductVO voFromReadModel(ProductReadModel readModel,
                                       Map<Long, List<ProductQueryRepository.ProductImageInfo>> imagesByProduct,
                                       Map<Long, SellerReadModel> sellerMap) {
        SellerReadModel seller = sellerMap.get(readModel.sellerId());
        String username = seller != null
                ? (seller.nickName() != null ? seller.nickName() : seller.username())
                : readModel.username();
        String userAvatar = seller != null ? seller.avatar() : readModel.userAvatar();

        List<ProductQueryRepository.ProductImageInfo> images = imagesByProduct.getOrDefault(readModel.id(), List.of());
        List<String> imageUrls = images.stream()
                .map(ProductQueryRepository.ProductImageInfo::imageUrl)
                .collect(Collectors.toList());
        String mainImageUrl = "";
        for (ProductQueryRepository.ProductImageInfo img : images) {
            if (img.isMain()) {
                mainImageUrl = img.imageUrl();
                break;
            }
        }
        if (mainImageUrl.isEmpty() && !imageUrls.isEmpty()) {
            mainImageUrl = imageUrls.getFirst();
        }
        ProductReadModel enriched = new ProductReadModel(
                readModel.id(),
                readModel.sellerId(),
                username,
                userAvatar,
                readModel.categoryId(),
                readModel.categoryName(),
                readModel.title(),
                readModel.description(),
                readModel.price(),
                readModel.originalPrice(),
                readModel.stock(),
                readModel.status(),
                readModel.statusDesc(),
                readModel.views(),
                readModel.condition(),
                readModel.conditionDesc(),
                readModel.location(),
                readModel.contactMethod(),
                imageUrls,
                mainImageUrl,
                readModel.createTime(),
                readModel.updateTime()
        );
        return readModelAssembler.toProductVO(enriched);
    }
}
