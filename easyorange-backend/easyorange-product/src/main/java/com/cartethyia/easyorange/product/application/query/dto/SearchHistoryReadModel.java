package com.cartethyia.easyorange.product.application.query.dto;

import java.time.LocalDateTime;

public record SearchHistoryReadModel(
    Long id,
    String keyword,
    LocalDateTime createTime
) {}
