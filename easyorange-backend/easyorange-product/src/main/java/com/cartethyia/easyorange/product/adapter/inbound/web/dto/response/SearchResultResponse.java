package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SearchResultResponse(
    String id,
    String title,
    BigDecimal price,
    BigDecimal originalPrice,
    String mainImageUrl,
    Integer status,
    String statusDesc,
    Integer condition,
    String conditionDesc,
    String location,
    LocalDateTime createTime
) { }
