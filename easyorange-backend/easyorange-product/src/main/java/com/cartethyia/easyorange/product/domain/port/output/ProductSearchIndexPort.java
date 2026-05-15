package com.cartethyia.easyorange.product.domain.port.output;

/**
 * 商品搜索索引端口
 *
 * <p>用于商品事件发生后同步更新搜索引擎（如 Elasticsearch）中的索引。
 * 由 application 模块实现，product 模块内通过 Optional 注入解耦。</p>
 */
public interface ProductSearchIndexPort extends OutboundPort {

    /**
     * 为新建商品创建搜索索引
     */
    void indexProduct(Long productId);

    /**
     * 更新已变更商品的搜索索引
     */
    void updateProductIndex(Long productId);

    /**
     * 删除商品的搜索索引
     */
    void removeProductIndex(Long productId);
}
