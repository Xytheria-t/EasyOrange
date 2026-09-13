package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.domain.valueobject.StockChange;
import com.cartethyia.easyorange.product.domain.valueobject.StockDrift;
import java.util.List;

/**
 * 库存流水仓储 — 库存变更的唯一落账出口，与库存余额更新同事务提交。
 * <p>
 * 落账即幂等：{@link #recordIfAbsent} 依赖唯一索引 {@code (change_type, biz_id, product_id)} 抢占落账权，
 * 返回 {@code false} 表示这笔变更此前已生效（重复投递 / DLQ 重放），调用方必须跳过本次库存变更与事件发布。
 * 于是重复投递的后果是「少做一次」而不是「多加一件库存」——幂等证明落在数据库唯一键上，不依赖消息中间件。
 */
public interface StockLedgerRepository {

    /**
     * 落账无业务单号的变更（{@link com.cartethyia.easyorange.product.domain.enums.StockChangeType#INIT 初始化}
     * / {@link com.cartethyia.easyorange.product.domain.enums.StockChangeType#ADJUST 人工调整}）——这类变更天然可重复，
     * 不参与幂等约束。
     */
    void record(StockChange change);

    /**
     * 幂等落账（下单扣减 / 取消退款恢复）。
     *
     * @return {@code true} 本次为首次生效；{@code false} 该变更已落账，调用方应跳过库存变更
     */
    boolean recordIfAbsent(StockChange change);

    /**
     * 对账扫描：返回库存余额与最近一条流水余额快照不一致的资产。
     *
     * @param limit 单次最多返回条数（异常时避免刷爆日志与告警）
     */
    List<StockDrift> findDrifts(int limit);
}
