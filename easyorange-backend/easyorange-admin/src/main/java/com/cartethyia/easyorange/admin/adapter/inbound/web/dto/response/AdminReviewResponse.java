package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdminReviewResponse(
    String reviewId,
    String productId,
    String productName,
    String userId,
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