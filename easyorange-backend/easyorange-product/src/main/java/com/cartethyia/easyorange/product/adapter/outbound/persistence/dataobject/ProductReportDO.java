package com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_product_report")
public class ProductReportDO extends BaseDO {

    private Long productId;

    private Long reporterId;

    private String reason;

    private Integer status;

    private String handleResult;
}
