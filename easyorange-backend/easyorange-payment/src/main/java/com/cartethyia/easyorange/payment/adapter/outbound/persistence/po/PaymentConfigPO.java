package com.cartethyia.easyorange.payment.adapter.outbound.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cartethyia.easyorange.common.entity.BaseDO;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@TableName("eo_payment_config")
public class PaymentConfigPO extends BaseDO {

    private String channelCode;

    private String channelName;

    private String appId;

    @JsonIgnore
    private String privateKey;

    @JsonIgnore
    private String publicKey;

    private Boolean sandbox;

    private Integer status;

    private String remark;

    @Version
    private Integer version;
}
