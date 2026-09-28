package com.cartethyia.easyorange.adapter.outbound.product;

import com.cartethyia.easyorange.ai.domain.model.CategorySummary;
import com.cartethyia.easyorange.ai.domain.port.CategoryCatalogPort;
import com.cartethyia.easyorange.ai.domain.port.CategoryListPort;
import com.cartethyia.easyorange.product.application.query.CategoryQueryHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * 公开类目适配器 — 同时实现 ai 模块的两个类目端口（MCP 工具面浏览 + 拍照识别清单）。
 * <p>
 * 两个端口此前各有一条实现：{@code CategoryListPort} 走 {@link CategoryQueryHandler}（带缓存、
 * 经 read model），{@code CategoryCatalogPort} 却是一段裸 SQL 直接查 {@code eo_category}。
 * 同一张表两个事实来源，过滤规则（level / status）各写一遍 —— 迟早漂。
 * 现在统一走一条路径：类目怎么过滤只有 product 模块说了算。
 * <p>
 * 「拍照识别只给一级类目」这个口径保留在 {@link #listAvailableCategoryNames} 里，
 * 因为它约束的是**模型**该选什么，不是数据该怎么读。
 */
@Component
@RequiredArgsConstructor
public class CategoryListAdapter implements CategoryListPort, CategoryCatalogPort {

    private final CategoryQueryHandler categoryQueryHandler;

    @Override
    public List<CategorySummary> list(@Nullable String parentId) {
        return categoryQueryHandler.getCategories(parentId).stream()
                .map(cat -> new CategorySummary(
                        cat.id(), cat.name(), cat.level() == null ? 0 : cat.level(), cat.productCount()))
                .toList();
    }

    /**
     * 拍照识别的可用类目名 — 只给<b>一级</b>类目。
     * <p>
     * 发布页下拉就是一级清单：喂二级叶子名（如「耳机音箱」）模型选得再准，前端也匹配不到 ID，
     * 类别回填必空。禁用类目同理，{@link #list} 已按 status 过滤，这里不必重复。
     */
    @Override
    public List<String> listAvailableCategoryNames() {
        return list(null).stream().map(CategorySummary::name).toList();
    }
}
