package com.cartethyia.easyorange.adapter.outbound.product;

import com.cartethyia.easyorange.ai.domain.model.CategorySummary;
import com.cartethyia.easyorange.ai.domain.port.CategoryListPort;
import com.cartethyia.easyorange.product.application.query.CategoryQueryHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * 公开类目适配器 — 实现 ai 模块定义的 {@link CategoryListPort}：MCP 工具面类目浏览
 * 逐层读取。经 product 模块 {@link CategoryQueryHandler}（带缓存）读
 * {@code CategoryReadModel}，收敛成仅公开字段的 {@link CategorySummary}
 * （与 {@code AssetDetailAdapter} 同向：端口由消费方定义，本模块翻译实现）。
 */
@Component
@RequiredArgsConstructor
public class CategoryListAdapter implements CategoryListPort {

    private final CategoryQueryHandler categoryQueryHandler;

    @Override
    public List<CategorySummary> list(@Nullable String parentId) {
        return categoryQueryHandler.getCategories(parentId).stream()
                .map(cat -> new CategorySummary(
                        cat.id(), cat.name(), cat.level() == null ? 0 : cat.level(), cat.productCount()))
                .toList();
    }
}
