package com.cartethyia.easyorange.product.adapter.outbound.persistence.search;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HotKeywordMapper extends BaseMapper<HotKeywordDO> {

    /**
     * 批量 upsert 热词计数。
     * <p>
     * {@code search_count} 走累加而非覆盖：调用方攒的是「上次刷入之后的新增量」，
     * 覆盖写会让每次刷入把历史次数清零。
     */
    void batchInsertOrUpdate(@Param("list") List<HotKeywordDO> keywords);
}
