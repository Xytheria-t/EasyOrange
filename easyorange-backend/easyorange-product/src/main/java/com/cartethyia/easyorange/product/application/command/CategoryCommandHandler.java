package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.product.application.port.cache.CategoryCachePort;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.domain.aggregate.Category;
import com.cartethyia.easyorange.product.domain.enums.CategoryStatus;
import com.cartethyia.easyorange.product.domain.exception.ProductDomainException;
import com.cartethyia.easyorange.product.domain.repository.CategoryRepository;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryName;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 分类写侧 — 增删改与状态切换，**不变量在这里编排**。
 * <p>
 * 分工：单个节点内的判定（层级算术、环、深度）由 {@link Category} 自己判；需要查库的判定
 * （同级重名、子树节点集合、关联商品数）由本类取数后交给聚合或直接拒绝。
 * <p>
 * 写完统一 {@code evictAll} 整份分类缓存：类目树是低频写（运营偶尔调一次），
 * 逐 key 枚举受影响的 level/parent 组合极易漏（漏一个就是脏数据），整份失效的代价可忽略。
 */
@Service
@RequiredArgsConstructor
public class CategoryCommandHandler {

    private final CategoryRepository categoryRepository;
    private final CategoryQueryRepository categoryQueryRepository;
    private final CategoryCachePort categoryCachePort;
    private final IdGenerator idGenerator;

    // ── 写操作 ──

    /**
     * 新建分类 — 层级由父分类推导，同级重名在此拦截。
     *
     * @param parentId 父分类 id；null 表示建一级分类
     */
    @Transactional(rollbackFor = Exception.class)
    public Category createCategory(String name, String parentId, String icon, Integer sortOrder) {
        Category parent = resolveParent(parentId);
        requireUniqueName(name, parentId, null);

        Category created = Category.create(
                CategoryId.of(idGenerator.generateId()), CategoryName.of(name), parent, icon, sortOrder);
        Category saved = categoryRepository.insert(created);
        categoryCachePort.evictAll();
        return saved;
    }

    /**
     * 更新分类属性（名称 / 排序 / 图标 / 状态）—— **不移动挂载点**。
     * <p>
     * 移动是另一条路径（{@link #moveCategory}）：两者混在一起正是旧实现出 bug 的根源 ——
     * 「移动到根」要把 parent_id 写成 NULL，而 MyBatis-Plus 默认跳过 null 字段，
     * 混在 update 里就静默丢了这次变更。
     */
    @Transactional(rollbackFor = Exception.class)
    public Category updateCategory(String id, String name, Integer sortOrder, String icon, Integer status) {
        Category updated = findOrThrow(id);

        if (name != null && !name.equals(updated.getName().value())) {
            String parentId =
                    updated.getParentId() != null ? updated.getParentId().value() : null;
            requireUniqueName(name, parentId, id);
            updated = updated.rename(CategoryName.of(name));
        }
        if (sortOrder != null) {
            updated = updated.changeSortOrder(sortOrder);
        }
        if (icon != null) {
            updated = updated.changeIcon(icon);
        }
        if (status != null) {
            updated = updated.changeStatus(CategoryStatus.fromCode(String.valueOf(status)));
        }

        Category saved = categoryRepository.update(updated);
        categoryCachePort.evictAll();
        return saved;
    }

    /**
     * 移动分类挂载点 — 环检测 + 整棵子树的 level 平移都在这里收口。
     * <p>
     * 旧实现只改被移动节点自身的 level，子孙留在旧层级，于是「level」字段从此不可信
     * （一级分类挂着二级子类），且 MAX_LEVEL 校验可被反复移动绕过。
     *
     * @param newParentId 新的父分类 id；null 表示移到一级
     */
    @Transactional(rollbackFor = Exception.class)
    public Category moveCategory(String id, String newParentId) {
        Category existing = findOrThrow(id);
        Category newParent = resolveParent(newParentId);

        // 一次性载入整棵子树：类目量级是百级，全量进内存比逐层回溯查库更快也更好推理
        List<Category> subtree = loadSubtree(existing);
        int subtreeHeight = subtreeHeight(subtree, existing.getLevel());

        List<CategoryId> subtreeIds = subtree.stream().map(Category::getId).toList();
        Category moved = existing.moveTo(newParent, subtreeIds, subtreeHeight);
        Category saved = categoryRepository.update(moved);

        // 子孙的 level 整体下移：旧 level 减去「自身旧 level」加上「自身新 level」，
        // 相对深度在子树内保持不变，一次遍历即可算完
        int levelDelta = saved.getLevel() - existing.getLevel();
        if (levelDelta != 0) {
            for (Category descendant : subtree) {
                if (descendant.getId().equals(existing.getId())) {
                    continue;
                }
                categoryRepository.update(descendant.withLevel(descendant.getLevel() + levelDelta));
            }
        }

        categoryCachePort.evictAll();
        return saved;
    }

    /** 启用/禁用分类。 */
    @Transactional(rollbackFor = Exception.class)
    public void updateCategoryStatus(String id, CategoryStatus status) {
        Category existing = findOrThrow(id);
        categoryRepository.update(existing.changeStatus(status));
        categoryCachePort.evictAll();
    }

    /**
     * 删除分类 — 有子分类或有关联商品一律拒绝。
     * <p>
     * 商品数用「不限上下架状态」口径：按在售统计会让只挂下架/已售商品的分类被当成空分类删掉，
     * 留下悬空 category_id（表上没有外键，数据库不会拦）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(String id) {
        Category existing = findOrThrow(id);

        if (!categoryRepository.findChildren(CategoryId.of(id)).isEmpty()) {
            throw ProductDomainException.categoryHasChildren(id);
        }

        Map<String, Long> productCounts = categoryQueryRepository.countAllProductsByCategoryIds(List.of(id));
        long productCount = productCounts.getOrDefault(id, 0L);
        if (productCount > 0) {
            throw ProductDomainException.categoryHasProducts(id, productCount);
        }

        categoryRepository.delete(existing.getId());
        categoryCachePort.evictAll();
    }

    // ── 私有 ──

    private Category resolveParent(String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return null;
        }
        return categoryRepository
                .findById(CategoryId.of(parentId))
                .orElseThrow(() -> ProductDomainException.categoryParentNotFound(parentId));
    }

    private Category findOrThrow(String id) {
        return categoryRepository
                .findById(CategoryId.of(id))
                .orElseThrow(() -> ProductDomainException.categoryNotFound(id));
    }

    /** 同级重名判定 —— 根节点用 {@code parentId == null}，与 {@code eo_category.parent_id} 同口径。 */
    private void requireUniqueName(String name, String parentId, String excludeId) {
        boolean duplicated = categoryRepository
                .findSiblings(parentId == null || parentId.isBlank() ? null : CategoryId.of(parentId), excludeId)
                .stream()
                .anyMatch(sibling -> sibling.getName().value().equals(name));
        if (duplicated) {
            throw ProductDomainException.categoryDuplicatedName(name, parentId);
        }
    }

    /** 载入自身及其全部子孙（含禁用节点：脏数据也得能算清层级）。 */
    private List<Category> loadSubtree(Category root) {
        List<Category> collected = new java.util.ArrayList<>();
        collected.add(root);
        collectDescendants(CategoryId.of(root.getId().value()), collected);
        return collected;
    }

    private void collectDescendants(CategoryId parentId, List<Category> collected) {
        for (Category child : categoryRepository.findChildren(parentId)) {
            collected.add(child);
            collectDescendants(child.getId(), collected);
        }
    }

    /**
     * 子树高度（自身记 1）—— 移动后最深层级 = 新挂载层级 + 高度 - 1，超过 MAX_LEVEL 即拒绝。
     */
    private int subtreeHeight(List<Category> subtree, int selfLevel) {
        int deepest = selfLevel;
        for (Category node : subtree) {
            if (node.getLevel() != null) {
                deepest = Math.max(deepest, node.getLevel());
            }
        }
        return Math.max(1, deepest - selfLevel + 1);
    }
}
