package com.cartethyia.easyorange.product.application.port.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.query.criteria.ProductSearchCriteria;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;

import java.util.List;
import java.util.Set;

public interface ProductQueryRepository {

    PageResult<ProductReadModel> searchProducts(ProductSearchCriteria criteria);

    PageResult<ProductReadModel> findProductsBySellerId(String sellerId, String status,
                                                    Integer pageNum, Integer pageSize);

    List<ProductReadModel> findProductsByIds(List<String> ids);

    ProductReadModel findProductById(String id);

    List<SearchHistoryReadModel> findSearchHistoryByUserId(String userId, Integer limit);

    List<HotKeywordReadModel> findHotKeywords(Integer limit);

    List<String> findSearchSuggestions(String keyword, Integer limit);

    List<SellerReadModel> findSellersByIds(Set<String> sellerIds);

    List<CategoryInfo> findCategoriesByIds(List<String> categoryIds);

    List<ProductDetailInfo> findDetailsByProductIds(List<String> productIds);

    List<ProductImageInfo> findImagesByProductIds(List<String> productIds);

    void saveSearchHistory(String userId, String keyword);

    void clearSearchHistory(String userId);

    void deleteSearchHistoryById(String historyId, String userId);

    long countByStatus(String status);

    record CategoryInfo(String id, String name, String parentId, Integer level, Integer sortOrder) { }

    record ProductDetailInfo(String productId, String description) { }

    record ProductImageInfo(String productId, String imageUrl, Integer sortOrder, boolean isMain) { }
}
