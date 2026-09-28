package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.domain.aggregate.Category;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import java.util.List;
import java.util.Optional;

/**
 * 分类聚合仓储 — 写侧（增删改）与树结构查询。
 * <p>
 * 读侧（C 端列表、计数、MCP 工具面）走 {@code CategoryQueryRepository} + 缓存，刻意与本端口分离：
 * 高频读不该每次都过聚合装配，也就不该被写侧的乐观锁与事件拖累。
 */
public interface CategoryRepository {

    /** 新增分类（主键由适配器分配）。 */
    Category insert(Category category);

    /** 更新分类（保留主键与乐观锁版本）。 */
    Category update(Category category);

    /** 逻辑删除。 */
    void delete(CategoryId id);

    Optional<Category> findById(CategoryId id);

    List<Category> findByIds(List<CategoryId> ids);

    /** 查某分类的直接子节点（含禁用，删除前的存在性校验需要看到禁用子节点）。 */
    List<Category> findChildren(CategoryId parentId);

    /**
     * 查某分类子树内的全部 id（**含自身**，按深度升序）—— 成环检测与整棵子树 level 平移的输入。
     *
     * @param root 起点；null 表示从根节点（parent_id IS NULL）起算，返回全树
     */
    List<CategoryId> findSubtreeIds(CategoryId root);

    /** 查同一父分类下的兄弟节点（parentId 为 null 即一级分类），用于同级重名判定。 */
    List<Category> findSiblings(CategoryId parentId, String excludeId);

    /** 统计整个类目树的节点数。 */
    long countAll();

    /** 查全部分类（按 sortOrder 升序）—— 建树用，分类量级小（百级）无需分页。 */
    List<Category> findAll();
}
