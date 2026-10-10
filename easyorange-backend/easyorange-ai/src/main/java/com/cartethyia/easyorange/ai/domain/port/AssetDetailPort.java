package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import java.util.List;
import java.util.Optional;

/**
 * 在售资产详情端口 — {@code product_detail} 工具的执行通道；端口由 ai 定义、实现在
 * easyorange-application（经 product 模块查询仓储），方向与 {@link AssetRetrievalPort} 一致：
 * 只读消费 product 的数据，不碰其领域模型。
 */
public interface AssetDetailPort {

    /** 不存在（或已删除）时返回 empty，由调用方转成模型可读的观察。 */
    Optional<AssetDetail> findDetail(String productId);

    /**
     * 批量查资产详情（compare_assets 工具的通道）— 只返回<b>存在</b>的，缺失 ID 不出现、
     * 不抛异常，调用方按 id 对齐；默认实现逐个 {@link #findDetail}，正确但 N+1，适配器应覆写为
     * 三次批量查询（商品 / 描述 / 卖家）一次取回。
     */
    default List<AssetDetail> findDetails(List<String> productIds) {
        return productIds.stream()
                .map(this::findDetail)
                .flatMap(Optional::stream)
                .toList();
    }
}
