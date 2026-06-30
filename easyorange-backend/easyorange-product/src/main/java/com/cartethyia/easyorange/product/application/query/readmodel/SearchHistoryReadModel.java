package com.cartethyia.easyorange.product.application.query.readmodel;

import java.time.LocalDateTime;

public record SearchHistoryReadModel(
    String id,
    String keyword,
    LocalDateTime createTime
) { }
