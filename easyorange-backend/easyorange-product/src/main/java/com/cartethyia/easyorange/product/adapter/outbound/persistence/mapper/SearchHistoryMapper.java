package com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.SearchHistoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistoryDO> {

    void batchInsert(@Param("list") List<SearchHistoryDO> list);
}
