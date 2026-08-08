package com.cartethyia.easyorange.ai.service;

import com.cartethyia.easyorange.ai.budget.TokenBudget;
import com.cartethyia.easyorange.ai.enums.AiCallScope;
import com.cartethyia.easyorange.ai.dto.SemanticSearchResult;
import com.cartethyia.easyorange.product.application.port.query.ProductSearchQueryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final EmbeddingModel embeddingModel;
    private final Optional<ProductSearchQueryPort> searchQueryPort;
    private final AiModelSupport aiModelSupport;

    @TokenBudget(scenario = "semantic", maxTokensPerCall = 500, dailyTokenLimit = 200_000)
    public SemanticSearchResult search(String keyword, int pageNum, int pageSize) {
        if (keyword == null || keyword.isBlank()) {
            return SemanticSearchResult.empty(pageNum, pageSize);
        }

        if (searchQueryPort.isEmpty()) {
            log.warn("Semantic search unavailable: ES search adapter not configured");
            return SemanticSearchResult.empty(pageNum, pageSize);
        }

        List<Float> embedding = aiModelSupport.embed(embeddingModel, AiCallScope.SEMANTIC, keyword);
        if (embedding.isEmpty()) {
            log.warn("Empty embedding generated for keyword: {}", keyword);
            return SemanticSearchResult.empty(pageNum, pageSize);
        }

        var query = new ProductSearchQueryPort.ProductSearchQuery(
                keyword, null, null, null, null, null, null, pageNum, pageSize, embedding, true);

        var result = searchQueryPort.get().search(query);

        return new SemanticSearchResult(result.records(), result.total(), result.current(), result.size());
    }
}
