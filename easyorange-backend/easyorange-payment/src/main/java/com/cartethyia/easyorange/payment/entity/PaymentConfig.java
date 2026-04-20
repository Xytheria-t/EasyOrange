package com.cartethyia.easyorange.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 支付渠道配置实体
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_payment_config")
public class PaymentConfig extends BaseDO {

    /** 渠道编码，如 alipay、wechat */
    private String channelCode;

    /** 渠道名称 */
    private String channelName;

    /** 应用ID */
    private String appId;

    /** 商户私钥 */
    @JsonIgnore
    private String privateKey;

    /** 商户公钥 */
    @JsonIgnore
    private String publicKey;

    /** 是否沙箱环境 */
    private Boolean sandbox;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 配置备注 */
    private String remark;
}
