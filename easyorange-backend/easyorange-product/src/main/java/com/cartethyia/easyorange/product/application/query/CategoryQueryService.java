package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import com.cartethyia.easyorange.product.domain.port.CategoryCachePort;
import com.cartethyia.easyorange.product.domain.repository.query.CategoryQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryQueryService {

    private final CategoryCachePort<CategoryReadModel> categoryCachePort;
    private final CategoryQueryRepository categoryQueryRepository;

    @Transactional(readOnly = true)
    public List<CategoryReadModel> getCategories(String parentId) {
        List<CategoryReadModel> categories;
        if (parentId != null) {
            categories = categoryCachePort.getCategoriesByParentId(parentId);
        } else {
            categories = categoryCachePort.getCategoriesByLevel(1);
        }

        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        List<String> categoryIds = categories.stream()
                .map(CategoryReadModel::id)
                .filter(Objects::nonNull)
                .toList();

        Map<String, Long> productCountMap = categoryQueryRepository.countProductsByCategoryIds(categoryIds);

        return categories.stream()
                .map(cat -> new CategoryReadModel(
                        cat.id(),
                        cat.name(),
                        cat.parentId(),
                        cat.level(),
                        cat.icon(),
                        cat.sortOrder(),
                        cat.status(),
                        cat.createTime(),
                        productCountMap.getOrDefault(cat.id(), 0L).intValue()))
                .collect(Collectors.toList());
    }
}
