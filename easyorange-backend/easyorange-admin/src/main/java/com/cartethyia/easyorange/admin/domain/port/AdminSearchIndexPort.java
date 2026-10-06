package com.cartethyia.easyorange.admin.domain.port;

/**
 * Admin 模块的商品搜索索引端口
 * 用于触发 ES 商品索引全量重建，遵循防腐层原则
 */
public interface AdminSearchIndexPort {

    /**
     * 全量重建商品搜索索引，返回写入索引的商品数
     */
    int reindexAll();
}
