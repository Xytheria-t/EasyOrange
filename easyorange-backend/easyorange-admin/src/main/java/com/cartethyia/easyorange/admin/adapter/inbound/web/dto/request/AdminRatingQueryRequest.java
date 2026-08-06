package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminRatingQueryRequest {

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
