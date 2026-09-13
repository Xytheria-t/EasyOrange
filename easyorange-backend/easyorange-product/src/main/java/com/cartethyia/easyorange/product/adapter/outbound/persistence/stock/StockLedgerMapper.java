package com.cartethyia.easyorange.product.adapter.outbound.persistence.stock;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.product.domain.valueobject.StockDrift;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockLedgerMapper extends BaseMapper<StockLedgerDO> {

    /**
     * 幂等落账 — 唯一键冲突时退化为无操作。
     *
     * @return 影响行数：1 = 落账成功；0 = 该变更已存在（重复投递），调用方应跳过
     */
    int insertIfAbsent(StockLedgerDO entry);

    /** 对账扫描：返回实际库存与最近一条流水余额快照不一致的资产。 */
    List<StockDrift> selectDrifts(@Param("limit") int limit);
}
