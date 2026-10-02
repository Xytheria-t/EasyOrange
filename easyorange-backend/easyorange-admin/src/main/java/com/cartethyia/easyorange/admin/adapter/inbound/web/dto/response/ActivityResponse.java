package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import lombok.Builder;

@Builder
public record ActivityResponse(String time, String text, String type) {}
