package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.product.application.port.cache.CategoryCachePort;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryQueryHandler {

    private final CategoryCachePort categoryCachePort;
    private final CategoryQueryRepository categoryQueryRepository;

    /**
     * 取某父分类下的启用中子分类（含商品计数）。
     *
     * @param parentId 父分类 id；null / 空表示一级分类
     */
    @Transactional(readOnly = true)
    public List<CategoryReadModel> getCategories(String parentId) {
        String normalizedParentId = (parentId == null || parentId.isBlank()) ? null : parentId;
        List<CategoryReadModel> categories = categoryCachePort.getCategoriesByParentId(normalizedParentId);

        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        List<String> categoryIds = categories.stream()
                .map(CategoryReadModel::id)
                .filter(Objects::nonNull)
                .toList();

        // 统一用「含子分类」口径：递归 CTE 下叶子分类的结果与直接挂载计数相同（无子分类可聚合），
        // 一级分类则拿到整棵子树的聚合数。旧实现在这里按 parentId 是否为空分两个方法，
        // 差别只来自单层 JOIN 的能力限制，CTE 之后没有区别了。
        Map<String, Long> productCountMap =
                categoryQueryRepository.countOnlineProductsByCategoryIdsWithChildren(categoryIds);

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
