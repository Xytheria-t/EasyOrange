package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.CategorySummary;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * 公开类目查询端口 — 类目浏览工具的执行通道：逐层列出平台资产类目。
 * <p>
 * 端口由 ai 定义、实现在 easyorange-application（经 product 模块类目查询，带缓存），
 * 方向与 {@link AssetDetailPort} 一致：只读消费 product 的数据，不碰其领域模型。
 */
public interface CategoryListPort {

    /** {@code parentId} 为空返回一级类目，否则返回其直接子类目；计数为类目下在售资产数。 */
    List<CategorySummary> list(@Nullable String parentId);
}
