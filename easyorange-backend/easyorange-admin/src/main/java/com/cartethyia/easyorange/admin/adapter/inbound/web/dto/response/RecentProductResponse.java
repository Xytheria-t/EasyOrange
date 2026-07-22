package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RecentProductResponse {

    private String productId;

    private String name;

    private BigDecimal price;

    private String mainImage;

    private String status;

    private String statusDesc;

    private String sellerId;

    private String sellerName;

    private String categoryName;

    private Integer viewCount;

    private LocalDateTime createTime;
}