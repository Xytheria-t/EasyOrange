package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

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
