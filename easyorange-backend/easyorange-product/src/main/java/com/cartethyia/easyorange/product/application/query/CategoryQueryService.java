package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import com.cartethyia.easyorange.product.domain.port.CategoryCachePort;
import com.cartethyia.easyorange.product.domain.repository.query.CategoryQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryQueryService {

    private final CategoryCachePort categoryCachePort;
    private final CategoryQueryRepository categoryQueryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(Long parentId) {
        List<CategoryReadModel> categories;
        if (parentId != null) {
            categories = categoryCachePort.getCategoriesByParentId(parentId);
        } else {
            categories = categoryCachePort.getCategoriesByLevel(1);
        }

        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        List<Long> categoryIds = categories.stream()
                .map(CategoryReadModel::id)
                .toList();

        Map<Long, Long> productCountMap = categoryQueryRepository.countProductsByCategoryIds(categoryIds);

        return categories.stream()
                .map(cat -> CategoryResponse.builder()
                        .id(cat.id())
                        .name(cat.name())
                        .parentId(cat.parentId())
                        .level(cat.level())
                        .icon(cat.icon())
                        .sortOrder(cat.sortOrder())
                        .status(cat.status())
                        .createTime(cat.createTime())
                        .productCount(productCountMap.getOrDefault(cat.id(), 0L).intValue())
                        .build())
                .collect(Collectors.toList());
    }
}
