package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.dto.SemanticSearchResult;
import com.cartethyia.easyorange.ai.port.LlmPort;
import com.cartethyia.easyorange.product.domain.port.ProductSearchQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final LlmPort llmPort;
    private final Optional<ProductSearchQueryPort> searchQueryPort;

    public SemanticSearchResult search(String keyword, int pageNum, int pageSize) {
        if (keyword == null || keyword.isBlank()) {
            return SemanticSearchResult.empty(pageNum, pageSize);
        }

        if (searchQueryPort.isEmpty()) {
            log.warn("Semantic search unavailable: ES search adapter not configured");
            return SemanticSearchResult.empty(pageNum, pageSize);
        }

        List<Float> embedding = llmPort.generateEmbedding(keyword);
        if (embedding.isEmpty()) {
            log.warn("Empty embedding generated for keyword: {}", keyword);
            return SemanticSearchResult.empty(pageNum, pageSize);
        }

        var query = new ProductSearchQueryPort.ProductSearchQuery(
                keyword, null, null, null, null, null, null, pageNum, pageSize,
                embedding, true
        );

        var result = searchQueryPort.get().search(query);

        return new SemanticSearchResult(
                result.records(),
                result.total(),
                result.pageNum(),
                result.pageSize()
        );
    }
}