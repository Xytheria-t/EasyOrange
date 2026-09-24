package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
import java.util.List;
import java.util.Optional;

/**
 * 在售资产详情端口 — product_detail 工具的执行通道：按 ID 查单件在售资产详情。
 * <p>
 * 端口由 ai 定义、实现在 easyorange-application（经 product 模块查询仓储），方向与
 * {@link AssetRetrievalPort} 一致：只读消费 product 的数据，不碰其领域模型。
 */
public interface AssetDetailPort {

    /** 查资产详情；不存在（或已删除）时返回 empty，由调用方转成模型可读的观察。 */
    Optional<AssetDetail> findDetail(String productId);

    /**
     * 批量查资产详情（compare_assets 工具的通道）— 返回入参中**存在**的那些，
     * 调用方按 id 自行对齐缺失项。不存在的 ID 直接不出现在结果里，不抛异常。
     * <p>
     * 默认实现退化为逐个 {@link #findDetail}，正确但带 N+1；适配器应覆写为
     * 三次批量查询（商品 / 描述 / 卖家）一次取回。
     */
    default List<AssetDetail> findDetails(List<String> productIds) {
        return productIds.stream()
                .map(this::findDetail)
                .flatMap(Optional::stream)
                .toList();
    }
}
