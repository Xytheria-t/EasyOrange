package com.cartethyia.easyorange.product.interfaces.rest.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SearchResultResponse(
    Long id,
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
) {}
