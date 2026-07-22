package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TopProductResponse {
    private String productId;
    private String name;
    private Integer viewCount;
    private BigDecimal price;
    private String mainImage;
    private String status;
    private String statusDesc;
}