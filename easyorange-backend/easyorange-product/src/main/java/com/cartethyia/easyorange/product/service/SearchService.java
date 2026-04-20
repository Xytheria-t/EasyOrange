package com.cartethyia.easyorange.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cartethyia.easyorange.product.dto.request.ProductSearchRequest;
import com.cartethyia.easyorange.product.dto.vo.HotKeywordVO;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;
import com.cartethyia.easyorange.product.dto.vo.SearchHistoryVO;
import com.cartethyia.easyorange.product.entity.SearchHistory;

import java.util.List;

public interface SearchService extends IService<SearchHistory> {

    Page<ProductVO> searchProducts(ProductSearchRequest request);

    List<SearchHistoryVO> getMySearchHistory(Integer limit);

    void clearMySearchHistory();

    void deleteSearchHistory(Long historyId);

    List<HotKeywordVO> getHotKeywords(Integer limit);

    List<String> getSearchSuggestions(String keyword, Integer limit);

    void recordSearch(String keyword);
}
