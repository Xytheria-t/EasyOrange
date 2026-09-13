package com.cartethyia.easyorange.product.adapter.outbound.persistence.stock;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import com.cartethyia.easyorange.product.domain.enums.StockChangeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 库存流水 DO — append-only，不落 {@code @Version}（无并发更新语义：落账是插入，冲突由唯一索引裁决）。
 */
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@TableName("eo_stock_ledger")
public class StockLedgerDO extends BaseDO {

    private String productId;
    private String bizId;
    private StockChangeType changeType;
    private Integer delta;
    private Integer stockAfter;
}
