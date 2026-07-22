package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SearchResultResponse(
    String id,
    String title,
    BigDecimal price,
    BigDecimal originalPrice,
    String mainImageUrl,
    String status,
    String statusDesc,
    String condition,
    String conditionDesc,
    String location,
    LocalDateTime createTime
) { }
