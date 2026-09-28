package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.product.domain.enums.CategoryStatus;
import com.cartethyia.easyorange.product.domain.exception.ProductDomainException;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryId;
import com.cartethyia.easyorange.product.domain.valueobject.CategoryName;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 商品分类聚合根 — 平台的类目树（当前口径两级，允许配置到 {@link #MAX_LEVEL} 级）。
 * <p>
 * 聚合边界 = 一个分类节点，**不包含子节点**。子节点通过 {@link CategoryRepository} 按需加载：
 * 类目树是低频写、高频读的配置数据，把整棵树塞进一个聚合会让每次改一个叶子都要加载并重写全树。
 * 需要跨节点的判断（环、深度）由方法参数显式传入 {@code descendants} / {@code subtreeHeight}，
 * 由 {@code CategoryCommandHandler} 负责取数——领域对象只负责判定，不负责查询。
 * <p>
 * 内聚的不变量：
 * <ul>
 *   <li>名称非空且长度受限（{@link CategoryName}）</li>
 *   <li>层级 = 父层级 + 1，根节点为 1，且整棵子树不超过 {@link #MAX_LEVEL}</li>
 *   <li>移动不得成环（目标父分类不能在自身子树内）</li>
 *   <li>同级重名由 {@code CategoryCommandHandler} 查库判定（需要全量同级，非本聚合职责）</li>
 * </ul>
 */
@Getter
@Builder(toBuilder = true)
public class Category {

    /** 层级上限：平台类目树最深 3 级。 */
    public static final int MAX_LEVEL = 3;

    private final CategoryId id;
    private final CategoryName name;
    /** 父分类 id；一级分类为 null。 */
    private final CategoryId parentId;

    private final Integer level;
    private final String icon;
    private final Integer sortOrder;
    private final CategoryStatus status;
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;

    // ── 工厂 ──

    /**
     * 新建分类 — 层级由父分类决定（无父即 1 级），调用方需先校验重名。
     *
     * @param parent 父分类；null 表示建一级分类
     */
    public static Category create(CategoryId id, CategoryName name, Category parent, String icon, Integer sortOrder) {
        BizRequire.notNull(id, "分类ID不能为空");
        BizRequire.notNull(name, "分类名称不能为空");
        int level = parent != null ? parent.getLevel() + 1 : 1;
        if (level > MAX_LEVEL) {
            throw ProductDomainException.categoryLevelExceeded(level, MAX_LEVEL);
        }
        LocalDateTime now = LocalDateTime.now();
        return Category.builder()
                .id(id)
                .name(name)
                .parentId(parent != null ? parent.getId() : null)
                .level(level)
                .icon(icon)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .status(CategoryStatus.ENABLED)
                .createTime(now)
                .updateTime(now)
                .build();
    }

    // ── 变更 ──

    /**
     * 移动挂载点 — 环检测与深度校验都在这里，调用方只管传齐上下文。
     *
     * @param newParent        新的父分类；null 表示移到一级
     * @param descendants      本分类子树内的所有 id（含自身），用于成环检测
     * @param subtreeHeight    本分类子树的高度（自身为 1，叶子为 1）——移动后最深的子孙也要不越界
     */
    public Category moveTo(Category newParent, List<CategoryId> descendants, int subtreeHeight) {
        if (newParent == null) {
            // 移到根：整棵子树以根为基准重算
            if (subtreeHeight > MAX_LEVEL) {
                throw ProductDomainException.categoryLevelExceeded(subtreeHeight, MAX_LEVEL);
            }
            return toBuilder()
                    .parentId(null)
                    .level(1)
                    .updateTime(LocalDateTime.now())
                    .build();
        }

        BizRequire.requireTrue(newParent.getId() != null, "父分类ID不能为空");
        if (newParent.getId().equals(this.id) || descendants.contains(newParent.getId())) {
            throw ProductDomainException.categoryCycleDetected(
                    id.value(), newParent.getId().value());
        }

        int newLevel = newParent.getLevel() + 1;
        // 子树整体下移后，最深的那层 = 新挂载层级 + 子树高度 - 1
        int deepest = newLevel + subtreeHeight - 1;
        if (deepest > MAX_LEVEL) {
            throw ProductDomainException.categoryLevelExceeded(deepest, MAX_LEVEL);
        }
        return toBuilder()
                .parentId(newParent.getId())
                .level(newLevel)
                .updateTime(LocalDateTime.now())
                .build();
    }

    /** 移动后按新父层级下推整棵子树的 level（自身已由 {@link #moveTo} 算好，子孙由本方法平移）。 */
    public Category withLevel(int newLevel) {
        return toBuilder().level(newLevel).updateTime(LocalDateTime.now()).build();
    }

    /** 改名。 */
    public Category rename(CategoryName newName) {
        BizRequire.notNull(newName, "分类名称不能为空");
        return toBuilder().name(newName).updateTime(LocalDateTime.now()).build();
    }

    /** 改排序值。 */
    public Category changeSortOrder(Integer newSortOrder) {
        return toBuilder()
                .sortOrder(newSortOrder != null ? newSortOrder : 0)
                .updateTime(LocalDateTime.now())
                .build();
    }

    /** 改图标。 */
    public Category changeIcon(String newIcon) {
        return toBuilder().icon(newIcon).updateTime(LocalDateTime.now()).build();
    }

    /** 启用/禁用。 */
    public Category changeStatus(CategoryStatus newStatus) {
        BizRequire.notNull(newStatus, "分类状态不能为空");
        return toBuilder().status(newStatus).updateTime(LocalDateTime.now()).build();
    }

    /** 层级是否为一级。 */
    public boolean isRoot() {
        return parentId == null;
    }
}
