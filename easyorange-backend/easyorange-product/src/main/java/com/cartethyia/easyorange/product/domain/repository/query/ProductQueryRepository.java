package com.cartethyia.easyorange.product.domain.repository.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import com.cartethyia.easyorange.product.dto.vo.SearchHistoryVO;
import com.cartethyia.easyorange.product.dto.vo.HotKeywordVO;

import java.util.List;

public interface ProductQueryRepository {

    Page<ProductVO> searchProducts(String keyword, Long categoryId, Integer status,
                                   Integer pageNum, Integer pageSize);

    List<ProductVO> findProductsByIds(List<Long> ids);

    ProductVO findProductVOById(Long id);

    List<SearchHistoryVO> findSearchHistoryByUserId(Long userId, Integer limit);

    List<HotKeywordVO> findHotKeywords(Integer limit);

    List<String> findSearchSuggestions(String keyword, Integer limit);
}
