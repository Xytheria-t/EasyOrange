package com.cartethyia.easyorange.product.application.query.readmodel;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
public record ProductReadModel(
    String id,
    String sellerId,
    String username,
    String userAvatar,
    String categoryId,
    String categoryName,
    String title,
    String description,
    BigDecimal price,
    BigDecimal originalPrice,
    Integer stock,
    String status,
    String statusDesc,
    Integer views,
    String condition,
    String conditionDesc,
    String location,
    String contactMethod,
    List<String> images,
    String mainImageUrl,
    LocalDateTime createTime,
    LocalDateTime updateTime
) { }
