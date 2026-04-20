package com.cartethyia.easyorange.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 通用树形结构构建工具
 * <p>
 * 适用于分类树、菜单树、部门树等具有父子关系的层级数据。
 * 输入平铺列表，输出树形结构。
 * </p>
 *
 * <pre>{@code
 * // 用法示例
 * List<CategoryVO> tree = TreeUtil.buildTree(
 *     categories,
 *     CategoryVO::getId,
 *     CategoryVO::getParentId,
 *     CategoryVO::setChildren,
 *     0L  // 根节点 parentId
 * );
 * }</pre>
 *
 * @author cartethyia
 */
public final class TreeUtil {

    private TreeUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 构建树形结构
     *
     * @param list           平铺数据列表
     * @param idGetter       获取节点 ID
     * @param parentIdGetter 获取父节点 ID
     * @param childrenSetter 设置子节点列表
     * @param rootParentId   根节点的 parentId 值
     * @param <T>            节点类型
     * @param <ID>           ID 类型
     * @return 树形结构列表
     */
    public static <T, ID> List<T> buildTree(
            List<T> list,
            Function<T, ID> idGetter,
            Function<T, ID> parentIdGetter,
            TreeChildrenSetter<T> childrenSetter,
            ID rootParentId) {

        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        // 建立 ID → 节点映射
        Map<ID, T> idMap = new HashMap<>(list.size());
        for (T item : list) {
            ID id = idGetter.apply(item);
            if (id == null) {
                continue;
            }
            idMap.put(id, item);
        }

        // 建立 parentId → 子节点列表映射
        Map<ID, List<T>> parentIdMap = new HashMap<>();
        for (T item : list) {
            ID parentId = parentIdGetter.apply(item);
            if (parentId == null) {
                continue;
            }
            parentIdMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(item);
        }

        // 为每个节点设置子节点
        for (T item : list) {
            ID id = idGetter.apply(item);
            List<T> children = parentIdMap.get(id);
            childrenSetter.set(item, children != null ? children : Collections.emptyList());
        }

        // 返回根节点列表
        return parentIdMap.getOrDefault(rootParentId, Collections.emptyList());
    }

    /**
     * 子节点设置器函数式接口
     *
     * @param <T> 节点类型
     */
    @FunctionalInterface
    public interface TreeChildrenSetter<T> {
        void set(T node, List<T> children);
    }
}
