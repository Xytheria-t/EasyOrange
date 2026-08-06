package com.cartethyia.easyorange.product.adapter.outbound.persistence.search;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistoryDO> {

    void batchInsert(@Param("list") List<SearchHistoryDO> list);
}
