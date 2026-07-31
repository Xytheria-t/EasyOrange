package com.cartethyia.easyorange.product.adapter.outbound.persistence.search;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@TableName("eo_hot_keyword")
public class HotKeywordDO extends BaseDO {

    private String keyword;
    private Integer searchCount;
    private LocalDateTime lastSearchTime;
}
