package com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.HotKeywordDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface HotKeywordMapper extends BaseMapper<HotKeywordDO> {

    @Update("UPDATE hot_keyword SET search_count = search_count + 1, last_search_time = NOW() WHERE keyword = #{keyword}")
    int incrementSearchCount(@Param("keyword") String keyword);

    void batchInsertOrUpdate(@Param("list") java.util.List<HotKeywordDO> keywords);
}
