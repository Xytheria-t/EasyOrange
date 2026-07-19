package com.cartethyia.easyorange.product.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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

    private String productId;

    private String reporterId;

    private String reason;

    private Integer reasonType;

    private Integer status;

    private String handleResult;

    @Version
    private Integer version;
}
