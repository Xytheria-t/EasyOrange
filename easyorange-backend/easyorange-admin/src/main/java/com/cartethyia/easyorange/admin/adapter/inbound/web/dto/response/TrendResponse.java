package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import lombok.Builder;

@Builder
public record TrendResponse(String month, Long users, Long products, Long orders) {}
