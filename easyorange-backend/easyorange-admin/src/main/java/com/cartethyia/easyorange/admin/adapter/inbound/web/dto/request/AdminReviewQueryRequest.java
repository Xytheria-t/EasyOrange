package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminReviewQueryRequest {

    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String productId;
    private String userId;
    private Integer rating;
    private Integer status;
    private String keyword;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
