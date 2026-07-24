package com.cartethyia.easyorange.payment.adapter.inbound.web.request;

import com.cartethyia.easyorange.common.dto.PageRequest;
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
public class QueryPaymentRequest extends PageRequest {

    private String paymentNo;

    private String orderId;

    private String userId;

    private String status;

    private String paymentMethod;
}
