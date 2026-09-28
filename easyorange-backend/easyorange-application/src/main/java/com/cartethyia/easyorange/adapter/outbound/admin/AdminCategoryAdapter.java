package com.cartethyia.easyorange.adapter.outbound.admin;

import com.cartethyia.easyorange.admin.domain.model.CategoryView;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryPort;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.domain.aggregate.Category;
import com.cartethyia.easyorange.product.domain.enums.CategoryStatus;
import com.cartethyia.easyorange.product.domain.repository.CategoryRepository;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Admin 分类查询适配器 — 实现 {@link AdminCategoryPort}。
 * <p>
 * 读 product 模块的仓储与查询端口，翻译成 admin 侧只含 JDK 类型的 {@link CategoryView}（ACL 边界）。
 * <p>
 * 商品计数统一走「含子分类」口径：递归 CTE 下叶子分类的结果与直接挂载一致，
 * 一级分类则拿到整棵子树的聚合数 —— 不再按 level 拆两批分别查（那是单层 JOIN 时代的补偿）。
 */
@Primary
@Component
@RequiredArgsConstructor
public class AdminCategoryAdapter implements AdminCategoryPort {

    private final CategoryRepository categoryRepository;
    private final CategoryQueryRepository categoryQueryRepository;

    @Override
    public java.util.Optional<CategoryView> getCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return java.util.Optional.empty();
        }
        return categoryRepository.findById(CategoryId.of(categoryId)).map(this::toView);
    }

    @Override
    public List<CategoryView> listCategories(String parentId, boolean includeDisabled) {
        String normalized = (parentId == null || parentId.isBlank()) ? null : parentId;
        List<Category> rows = categoryRepository.findChildren(
                        normalized != null ? CategoryId.of(normalized) : null)
                .stream()
                .filter(category -> includeDisabled || category.getStatus().isEnabled())
                .toList();
        return enrich(rows);
    }

    @Override
    public List<CategoryView> categoryTree() {
        List<CategoryView> all = enrich(categoryRepository.findAll().stream()
                .filter(category -> category.getStatus().isEnabled())
                .toList());

        Map<String, List<CategoryView>> childrenByParent = all.stream()
                .filter(view -> view.parentId() != null)
                .collect(Collectors.groupingBy(CategoryView::parentId, LinkedHashMap::new, Collectors.toList()));
        // 一级分类的 parentId 是 null，单独挑出来当根 —— 直接用 null 做 map key 虽可行但语义不清
        List<CategoryView> roots = all.stream().filter(view -> view.parentId() == null).toList();
        return attachChildren(roots, childrenByParent);
    }

    private List<CategoryView> enrich(List<Category> rows) {
        List<String> ids = rows.stream().map(c -> c.getId().value()).toList();
        Map<String, Long> productCounts = categoryQueryRepository.countOnlineProductsByCategoryIdsWithChildren(ids);
        Map<String, String> parentNames = parentNameMap(rows);
        return rows.stream()
                .map(row -> toViewWithNames(row, productCounts, parentNames))
                .toList();
    }

    private Map<String, String> parentNameMap(List<Category> rows) {
        List<String> parentIds = rows.stream()
                .map(Category::getParentId)
                .filter(Objects::nonNull)
                .map(CategoryId::value)
                .distinct()
                .toList();
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        return categoryRepository.findByIds(parentIds.stream().map(CategoryId::of).toList()).stream()
                .collect(Collectors.toMap(
                        parent -> parent.getId().value(), parent -> parent.getName().value(), (a, b) -> a));
    }

    /**
     * 递归挂上子节点。
     * <p>
     * 环检测在写侧已保证（{@code Category.moveTo}），故这里无需防环；
     * 若分组里存在孤儿节点（父分类被禁用或被删），它不会出现在任何 children 里 ——
     * 后台看到的是「少了一条」，比死循环安全。
     */
    private List<CategoryView> attachChildren(List<CategoryView> nodes, Map<String, List<CategoryView>> childrenByParent) {
        return nodes.stream()
                .map(node -> withChildren(node, childrenByParent))
                .toList();
    }

    private CategoryView withChildren(CategoryView node, Map<String, List<CategoryView>> childrenByParent) {
        List<CategoryView> children = attachChildren(
                childrenByParent.getOrDefault(node.id(), List.of()), childrenByParent);
        return new CategoryView(
                node.id(),
                node.name(),
                node.parentId(),
                node.parentName(),
                node.level(),
                node.sortOrder(),
                node.status(),
                node.productCount(),
                node.createTime(),
                children);
    }

    private CategoryView toView(Category category) {
        return toViewWithNames(category, Map.of(), Map.of());
    }

    private CategoryView toViewWithNames(
            Category category, Map<String, Long> productCounts, Map<String, String> parentNames) {
        String parentId = category.getParentId() != null ? category.getParentId().value() : null;
        return new CategoryView(
                category.getId().value(),
                category.getName().value(),
                parentId,
                parentId != null ? parentNames.get(parentId) : null,
                category.getLevel(),
                category.getSortOrder(),
                statusCode(category.getStatus()),
                productCounts.getOrDefault(category.getId().value(), 0L),
                category.getCreateTime(),
                List.of());
    }

    /** domain 枚举 → admin 视图的数字状态（0/1），保持与既有前端契约一致。 */
    private static Integer statusCode(CategoryStatus status) {
        return status != null && status.isEnabled() ? 1 : 0;
    }
}
