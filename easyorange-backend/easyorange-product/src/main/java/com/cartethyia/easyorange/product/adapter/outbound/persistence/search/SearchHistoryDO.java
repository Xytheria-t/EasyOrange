package com.cartethyia.easyorange.product.adapter.outbound.persistence.search;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@TableName("eo_search_history")
public class SearchHistoryDO extends BaseDO {

    private String userId;
    private String keyword;
    private LocalDateTime searchTime;
}
