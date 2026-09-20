package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cartethyia.easyorange.common.entity.BaseDO;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@TableName("eo_product")
public class ProductDO extends BaseDO {

    private String userId;
    private String categoryId;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;

    /** AI 建议快照（拍照识别产出，未识别则 null）JSON 原文。字段级采纳率与价格偏离度的数据来源 */
    private String aiSuggestion;

    private Integer stock;

    @Version
    private Integer version;

    private ProductStatus status;
    private Integer viewCount;
    private ConditionLevel conditionLevel;
    private String location;
    private String contactMethod;
    private String tags;
    private String searchText;
    private LocalDateTime priceUpdateTime;
}
