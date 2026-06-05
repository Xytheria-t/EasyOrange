package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrendResponse {

    private String month;

    private Long users;

    private Long products;

    private Long orders;
}