package com.cartethyia.easyorange.payment.adapter.inbound.web.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfigResponse {

    private Long id;

    private String channelCode;

    private String channelName;

    private Boolean sandbox;

    private Integer status;
}
