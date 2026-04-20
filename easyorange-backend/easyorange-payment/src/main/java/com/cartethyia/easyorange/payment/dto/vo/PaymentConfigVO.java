package com.cartethyia.easyorange.payment.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付渠道配置 VO
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfigVO {

    private Long id;

    private String channelCode;

    private String channelName;

    private Boolean sandbox;

    private Integer status;
}
