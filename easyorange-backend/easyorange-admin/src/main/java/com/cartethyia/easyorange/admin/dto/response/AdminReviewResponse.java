package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdminReviewResponse(
    Long reviewId,
    Long productId,
    String productName,
    Long userId,
    String username,
    String userAvatar,
    Integer rating,
    String content,
    String replyContent,
    Integer likes,
    Integer status,
    LocalDateTime createTime,
    LocalDateTime updateTime
) {}