package com.cartethyia.easyorange.product.application.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.dto.PageRequest;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.product.assembler.ProductAssembler;
import com.cartethyia.easyorange.product.application.query.ProductQuery;
import com.cartethyia.easyorange.product.dto.request.ProductQueryRequest;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import com.cartethyia.easyorange.product.entity.Category;
import com.cartethyia.easyorange.product.entity.Product;
import com.cartethyia.easyorange.product.entity.ProductDetail;
import com.cartethyia.easyorange.product.entity.ProductImage;
import com.cartethyia.easyorange.product.enums.ProductStatus;
import com.cartethyia.easyorange.product.mapper.ProductDetailMapper;
import com.cartethyia.easyorange.product.mapper.ProductMapper;
import com.cartethyia.easyorange.product.service.CategoryService;
import com.cartethyia.easyorange.product.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryHandler {

    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final ProductImageService productImageService;
    private final ProductDetailMapper productDetailMapper;
    private final ProductAssembler productAssembler;
    private final com.cartethyia.easyorange.product.application.cache.ProductCacheService productCacheService;

    @Transactional(readOnly = true)
    public ProductVO getProductById(Long id) {
        ProductVO cachedProduct = productCacheService.getProductCache(id);
        if (cachedProduct != null) {
            return cachedProduct;
        }
        
        Product product = productMapper.selectById(id);
        BizRequire.notNull(product, "商品不存在");
        ProductVO productVO = buildProductVO(product);
        
        productCacheService.setProductCache(id, productVO);
        return productVO;
    }

    @Transactional(readOnly = true)
    public List<ProductVO> getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Product> products = productMapper.selectBatchIds(ids);
        return buildProductVOPage(products, products.size(), 1, products.size()).records();
    }

    @Transactional(readOnly = true)
    public ProductVO handle(ProductQuery query) {
        ProductVO cachedProduct = productCacheService.getProductCache(query.getId());
        if (cachedProduct != null) {
            return cachedProduct;
        }
        
        Product product = productMapper.selectById(query.getId());
        BizRequire.notNull(product, "商品不存在");
        ProductVO productVO = buildProductVO(product);
        
        productCacheService.setProductCache(query.getId(), productVO);
        return productVO;
    }

    @Transactional(readOnly = true)
    public List<ProductVO> handleSimilarProducts(Long id, Integer limit) {
        Product product = productMapper.selectById(id);
        BizRequire.notNull(product, "商品不存在");

        Page<Product> page = new Page<>(1, limit);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getCategoryId, product.getCategoryId())
                .ne(Product::getId, id)
                .eq(Product::getStatus, ProductStatus.ONLINE.getCode())
                .orderByDesc(Product::getCreateTime);

        Page<Product> productPage = productMapper.selectPage(page, wrapper);
        return buildProductVOPage(productPage.getRecords(),
                (int) productPage.getTotal(),
                productPage.getCurrent(),
                productPage.getSize()).records();
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("price", "viewCount", "createTime");

    @Transactional(readOnly = true)
    public PageResult<ProductVO> handle(ProductQueryRequest request) {
        PageRequest normalized = request.normalized();
        int pageNum = normalized.getPageNum();
        int pageSize = normalized.getPageSize();

        if (StringUtils.isNotBlank(request.getKeyword())) {
            Page<Product> searchPage = new Page<>(pageNum, pageSize);
            Page<Product> resultPage = productMapper.searchByFullText(
                    searchPage, request.getKeyword(), ProductStatus.ONLINE.getCode());
            log.info("action=search_products keyword={} total={}", request.getKeyword(), resultPage.getTotal());
            return buildProductVOPage(resultPage.getRecords(),
                    (int) resultPage.getTotal(),
                    resultPage.getCurrent(),
                    resultPage.getSize());
        }

        Page<Product> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        if (request.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, request.getCategoryId());
        }
        if (request.getStatus() != null) {
            wrapper.eq(Product::getStatus, request.getStatus());
        } else {
            wrapper.eq(Product::getStatus, ProductStatus.ONLINE.getCode());
        }
        if (request.getConditionLevel() != null) {
            wrapper.eq(Product::getConditionLevel, request.getConditionLevel());
        }
        if (request.getMinPrice() != null) {
            wrapper.ge(Product::getPrice, request.getMinPrice());
        }
        if (request.getMaxPrice() != null) {
            wrapper.le(Product::getPrice, request.getMaxPrice());
        }

        applySorting(wrapper, request);

        Page<Product> productPage = productMapper.selectPage(page, wrapper);
        log.info("action=query_products categoryId={} total={}", request.getCategoryId(), productPage.getTotal());

        return buildProductVOPage(productPage.getRecords(),
                (int) productPage.getTotal(),
                productPage.getCurrent(),
                productPage.getSize());
    }

    private void applySorting(LambdaQueryWrapper<Product> wrapper, ProductQueryRequest request) {
        String sortField = request.validateSortField(ALLOWED_SORT_FIELDS);
        if (sortField == null) {
            sortField = "createTime";
        }

        boolean isAsc = request.isAsc();

        switch (sortField) {
            case "price" -> wrapper.orderBy(true, isAsc, Product::getPrice);
            case "viewCount" -> wrapper.orderBy(true, isAsc, Product::getViewCount);
            default -> wrapper.orderBy(true, isAsc, Product::getCreateTime);
        }
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

    private PageResult<ProductVO> buildProductVOPage(List<Product> products, int total, long current, long size) {
        if (products == null || products.isEmpty()) {
            return PageResult.empty((int) current, (int) size);
        }

        List<Long> productIds = products.stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        List<Long> categoryIds = productAssembler.collectCategoryIds(products);
        Set<Long> sellerIds = products.stream()
                .map(Product::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return buildProductVOPage(products, productIds, categoryIds, sellerIds, total, current, size);
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

    private PageResult<ProductVO> buildProductVOPage(List<Product> products, List<Long> productIds,
                                                     List<Long> categoryIds, Set<Long> sellerIds,
                                                     int total, long current, long size) {
        Map<Long, List<ProductImage>> imagesByProduct = productAssembler.groupImagesByProduct(
                productImageService.listByProductIds(productIds));
        Map<Long, Category> categoryMap = productAssembler.buildCategoryMap(
                categoryIds.isEmpty() ? List.of() : categoryService.listByIds(categoryIds));
        Map<Long, ProductDetail> detailMap = productAssembler.buildDetailMap(
                listDetailsByProductIds(productIds));
        Map<Long, com.cartethyia.easyorange.product.dto.vo.SellerInfo> sellerMap = productAssembler.buildSellerMap(
                sellerIds.isEmpty() ? List.of() : listSellersByIds(sellerIds));

        Page<ProductVO> voPage = productAssembler.toProductVOPage(products, imagesByProduct, categoryMap, detailMap, sellerMap,
                total, current, size);
        return PageResult.of(voPage.getRecords(), total, (int) current, (int) size);
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
