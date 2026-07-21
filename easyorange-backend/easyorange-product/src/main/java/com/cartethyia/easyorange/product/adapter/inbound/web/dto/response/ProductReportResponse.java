package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductReportResponse(
        String id,
        String productId,
        String reporterId,
        String reason,
        Integer reasonType,
        Integer status
) {}
