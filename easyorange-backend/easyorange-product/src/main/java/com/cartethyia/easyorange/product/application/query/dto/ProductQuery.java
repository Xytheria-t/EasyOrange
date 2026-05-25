package com.cartethyia.easyorange.product.application.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductQuery {

    private Long id;
    private Long userId;
    private Long categoryId;
    private Integer status;
    private String keyword;
    private Integer conditionLevel;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean hasDiscount;
    private String sortBy;
    private String sortOrder;
    private Integer pageNum;
    private Integer pageSize;
}
