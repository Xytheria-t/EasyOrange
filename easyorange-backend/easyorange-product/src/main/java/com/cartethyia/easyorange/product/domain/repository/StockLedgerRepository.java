package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.domain.valueobject.StockChange;
import com.cartethyia.easyorange.product.domain.valueobject.StockDrift;
import java.util.List;

/**
 * 库存流水仓储 — 库存变更的唯一落账出口，与库存余额更新同事务提交。
 * <p>
 * 幂等落账靠唯一索引 {@code (change_type, biz_id, product_id)} 抢占落账权，幂等证明落在数据库而非消息中间件：
 * 重复投递的后果是「少做一次」而不是「多加一件库存」。
 */
public interface StockLedgerRepository {

    /** 落账无业务单号的变更（INIT 初始化 / ADJUST 人工调整）— 天然可重复，不参与幂等约束。 */
    void record(StockChange change);

    /**
     * 幂等落账（下单扣减 / 取消退款恢复）。
     *
     * @return {@code true} 首次生效；{@code false} 该变更已落账，调用方须跳过库存变更与事件发布
     */
    boolean recordIfAbsent(StockChange change);

    /** 对账扫描：返回余额与最近一条流水快照不一致的资产。{@code limit} 为单次上限，异常时避免刷爆日志与告警。 */
    List<StockDrift> findDrifts(int limit);
}
