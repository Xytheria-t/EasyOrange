package com.cartethyia.easyorange.product.domain.repository.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface ProductQueryRepository {

    Page<ProductReadModel> searchProducts(String keyword, Long categoryId, Integer status,
                                           Integer pageNum, Integer pageSize);

    Page<ProductReadModel> searchProducts(String keyword, Long categoryId, Integer status,
                                          BigDecimal minPrice, BigDecimal maxPrice,
                                          Integer conditionLevel, String sort,
                                          Integer pageNum, Integer pageSize);

    Page<ProductReadModel> findProductsBySellerId(Long sellerId, Integer status,
                                                   Integer pageNum, Integer pageSize);

    List<ProductReadModel> findProductsByIds(List<Long> ids);

    ProductReadModel findProductById(Long id);

    List<SearchHistoryReadModel> findSearchHistoryByUserId(Long userId, Integer limit);

    List<HotKeywordReadModel> findHotKeywords(Integer limit);

    List<String> findSearchSuggestions(String keyword, Integer limit);

    List<SellerReadModel> findSellersByIds(Set<Long> sellerIds);

    List<CategoryInfo> findCategoriesByIds(List<Long> categoryIds);

    List<ProductDetailInfo> findDetailsByProductIds(List<Long> productIds);

    List<ProductImageInfo> findImagesByProductIds(List<Long> productIds);

    void saveSearchHistory(Long userId, String keyword);

    void clearSearchHistory(Long userId);

    void deleteSearchHistoryById(Long historyId, Long userId);

    record CategoryInfo(Long id, String name, Long parentId, Integer level, Integer sortOrder) { }

    record ProductDetailInfo(Long productId, String description) { }

    record ProductImageInfo(Long productId, String imageUrl, Integer sortOrder, boolean isMain) { }
}
