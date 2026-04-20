package com.cartethyia.easyorange.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 热词实体
 */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hot_keyword")
public class HotKeyword extends BaseDO {

    private String keyword;

    private Integer searchCount;

    private java.time.LocalDateTime lastSearchTime;
}
