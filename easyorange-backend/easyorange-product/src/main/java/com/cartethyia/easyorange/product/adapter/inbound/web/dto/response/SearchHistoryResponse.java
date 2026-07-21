package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchHistoryResponse(
        String id,
        String keyword,
        Integer searchCount,
        LocalDateTime createTime
) {}
