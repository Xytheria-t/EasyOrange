package com.cartethyia.easyorange.product.application.query.readmodel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductReadModel(
    Long id,
    Long sellerId,
    String username,
    String userAvatar,
    Long categoryId,
    String categoryName,
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
