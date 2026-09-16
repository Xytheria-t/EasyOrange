package com.cartethyia.easyorange.product.domain.port;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.valueobject.StockQuantity;
import java.util.List;

public interface ProductSnapshotPort {

    /**
     * 批量读快照 — 实现必须真批量（一次查齐），不得逐 id 循环查库。
     */
    List<ProductSnapshot> findSnapshots(List<ProductId> productIds);

    /**
     * 资产快照 — 实时状态（价格/状态/库存，下单校验与定价以此为准）+ 展示信息（标题/主图/描述/成色）。
     */
    record ProductSnapshot(
            ProductId productId,
            SellerId sellerId,
            Money price,
            ProductStatus status,
            StockQuantity stock,
            String title,
            String image,
            String description,
            String conditionLevel) {}
}
