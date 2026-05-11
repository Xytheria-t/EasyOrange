package com.cartethyia.easyorange.product.application.service;

import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import com.cartethyia.easyorange.product.domain.repository.query.ProductQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final ProductQueryRepository productQueryRepository;

    public void saveSearchHistory(Long userId, String keyword) {
        productQueryRepository.saveSearchHistory(userId, keyword);
    }

    @Transactional(readOnly = true)
    public List<SearchHistoryReadModel> getSearchHistory(Long userId, Integer limit) {
        return productQueryRepository.findSearchHistoryByUserId(userId, limit);
    }

    public void clearSearchHistory(Long userId) {
        productQueryRepository.clearSearchHistory(userId);
    }

    public void deleteSearchHistoryById(Long historyId, Long userId) {
        productQueryRepository.deleteSearchHistoryById(historyId, userId);
    }
}
