package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.model.AssetDetail;
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
}
