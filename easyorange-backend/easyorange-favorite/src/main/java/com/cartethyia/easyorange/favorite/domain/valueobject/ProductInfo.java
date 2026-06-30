package com.cartethyia.easyorange.favorite.domain.valueobject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductInfo(
    String id,
    String sellerId,
    String categoryId,
    String title,
    String description,
    BigDecimal price,
    BigDecimal originalPrice,
    Integer stock,
    Integer status,
    String statusDesc,
    Integer views,
    Integer condition,
    String conditionDesc,
    String location,
    String contactMethod,
    List<String> images,
    String mainImageUrl,
    LocalDateTime createTime,
    LocalDateTime updateTime
) { }
