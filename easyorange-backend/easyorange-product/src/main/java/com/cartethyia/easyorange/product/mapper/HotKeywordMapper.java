package com.cartethyia.easyorange.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.product.entity.HotKeyword;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface HotKeywordMapper extends BaseMapper<HotKeyword> {

    @Update("UPDATE hot_keyword SET search_count = search_count + 1, last_search_time = NOW() WHERE keyword = #{keyword}")
    int incrementSearchCount(@Param("keyword") String keyword);

    void batchInsertOrUpdate(@Param("list") java.util.List<HotKeyword> keywords);
}
