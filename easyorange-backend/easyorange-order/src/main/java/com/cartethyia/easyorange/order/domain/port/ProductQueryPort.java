package com.cartethyia.easyorange.order.domain.port;

import java.util.List;

public interface ProductQueryPort {

    List<ProductDetail> getProductsByIds(List<String> productIds);

    /**
     * 资产展示信息 — 订单读模型只消费这三个字段（id 建 map，标题与首图做列表展示）；
     * 价格、在架状态等下单校验字段走 {@link ProductInventoryPort} 的快照，不在此重复。
     */
    record ProductDetail(String id, String title, List<String> images) {}
}
